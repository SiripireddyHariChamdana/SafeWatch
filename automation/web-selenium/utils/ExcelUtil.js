const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

class ExcelUtil {
    constructor() {
        this.results = [];
    }

    logResult(id, module, name, status, priority, time) {
        this.results.push({ id, module, name, status, priority, time });
    }

    async generateReport(filePath) {
        const workbook = new ExcelJS.Workbook();
        const dir = path.dirname(filePath);
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

        const mainSheet = workbook.addWorksheet('Executed Test Cases');
        mainSheet.columns = [
            { header: 'Test ID', key: 'id' },
            { header: 'Module', key: 'module' },
            { header: 'Test Name', key: 'name' },
            { header: 'Priority', key: 'priority' },
            { header: 'Status', key: 'status' },
            { header: 'Execution Time', key: 'time' }
        ];
        this.results.forEach(res => mainSheet.addRow(res));

        // Create Filtered Sheets
        const createFiltered = (name, status) => {
            const sheet = workbook.addWorksheet(name);
            sheet.columns = mainSheet.columns;
            this.results.filter(r => r.status === status).forEach(r => sheet.addRow(r));
        };

        createFiltered('Passed Tests', 'PASS');
        createFiltered('Failed Tests', 'FAIL');
        createFiltered('Skipped Tests', 'SKIP');

        // Execution Metrics Sheet
        const metricsSheet = workbook.addWorksheet('Execution Metrics');
        const passCount = this.results.filter(r => r.status === 'PASS').length;
        const failCount = this.results.filter(r => r.status === 'FAIL').length;
        const skipCount = this.results.filter(r => r.status === 'SKIP').length;
        metricsSheet.addRow(['Metric', 'Value']);
        metricsSheet.addRow(['Total Executed', this.results.length]);
        metricsSheet.addRow(['Passed', passCount]);
        metricsSheet.addRow(['Failed', failCount]);
        metricsSheet.addRow(['Skipped', skipCount]);
        metricsSheet.addRow(['Pass Rate', ((passCount / (this.results.length || 1)) * 100).toFixed(2) + '%']);

        // Defect Summary Sheet
        const defectSheet = workbook.addWorksheet('Defect Summary');
        defectSheet.columns = [
            { header: 'Test ID', key: 'id' },
            { header: 'Module', key: 'module' },
            { header: 'Test Name', key: 'name' },
            { header: 'Failure Reason', key: 'reason' }
        ];
        this.results.filter(r => r.status === 'FAIL').forEach(r => {
            defectSheet.addRow({ id: r.id, module: r.module, name: r.name, reason: 'Assertion Failure / Element Not Found' });
        });

        // Pass Rate Summary Sheet
        const passRateSheet = workbook.addWorksheet('Pass Rate Summary');
        passRateSheet.addRow(['Module', 'Total', 'Passed', 'Failed', 'Pass Rate']);
        const modules = [...new Set(this.results.map(r => r.module))];
        modules.forEach(m => {
            const modResults = this.results.filter(r => r.module === m);
            const modPass = modResults.filter(r => r.status === 'PASS').length;
            const modFail = modResults.filter(r => r.status === 'FAIL').length;
            const rate = ((modPass / (modResults.length || 1)) * 100).toFixed(2) + '%';
            passRateSheet.addRow([m, modResults.length, modPass, modFail, rate]);
        });

        await workbook.xlsx.writeFile(filePath);
    }
}

module.exports = new ExcelUtil();
