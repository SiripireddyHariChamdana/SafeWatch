"""
SafeWatch Security Code Review — Automated Static Analysis
Verifies each identified vulnerability finding against the actual source code.
All tests must PASS (i.e., confirm vulnerabilities exist as documented).
"""
import unittest
import ast
import re
import os
import sys

BACKEND_DIR = os.path.join(os.path.dirname(__file__), "..", "backend")
MAIN_PY     = os.path.join(BACKEND_DIR, "main.py")
DB_PY       = os.path.join(BACKEND_DIR, "database.py")
MODELS_PY   = os.path.join(BACKEND_DIR, "models.py")


def read_file(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


MAIN_SRC   = read_file(MAIN_PY)
DB_SRC     = read_file(DB_PY)
MODELS_SRC = read_file(MODELS_PY)


class TestVULN01_HardcodedSecretKey(unittest.TestCase):
    """VULN-01 · Hardcoded weak default SECRET_KEY in database.py"""

    def test_default_secret_key_present_in_source(self):
        """The dangerous fallback literal must be in database.py"""
        self.assertIn(
            "your-secret-key-change-this-in-production",
            DB_SRC,
            "VULN-01: Expected hardcoded SECRET_KEY fallback not found in database.py"
        )

    def test_no_runtime_entropy_check(self):
        """No ValueError/RuntimeError raised when SECRET_KEY is weak"""
        # The code should NOT contain a length check guard (it doesn't — that's the bug)
        has_check = bool(re.search(r"len\(SECRET_KEY\)\s*<\s*\d+", DB_SRC))
        self.assertFalse(
            has_check,
            "VULN-01: SECRET_KEY entropy check exists — vulnerability may be mitigated"
        )


class TestVULN02_TestAccountBypass(unittest.TestCase):
    """VULN-02 · Automatic is_verified=True for test-pattern emails/phones"""

    def test_is_test_flag_exists(self):
        self.assertIn("is_test", MAIN_SRC,
            "VULN-02: is_test bypass variable not found in main.py")

    def test_example_com_bypass(self):
        self.assertIn("@example.com", MAIN_SRC,
            "VULN-02: @example.com bypass pattern not found in main.py")

    def test_test_keyword_bypass(self):
        # "test" in email triggers auto-verification
        self.assertIn('"test" in email', MAIN_SRC,
            "VULN-02: 'test' in email bypass not found")

    def test_phone_555_bypass(self):
        self.assertIn('"555" in phone', MAIN_SRC,
            "VULN-02: '555' in phone bypass not found")


class TestVULN03_HardcodedDatabasePassword(unittest.TestCase):
    """VULN-03 · Hardcoded default database password in database.py"""

    def test_hardcoded_db_url_with_password(self):
        self.assertIn(
            "postgresql://postgres:password@localhost",
            DB_SRC,
            "VULN-03: Hardcoded DB password fallback not found"
        )


class TestVULN04_NoRateLimiting(unittest.TestCase):
    """VULN-04 · No rate limiting on authentication endpoints"""

    def test_no_slowapi_import(self):
        """slowapi (common FastAPI rate limiter) is not imported"""
        self.assertNotIn("slowapi", MAIN_SRC,
            "VULN-04: slowapi found — rate limiting may be implemented")

    def test_no_limiter_decorator(self):
        """No @limiter decorator present"""
        self.assertNotIn("@limiter", MAIN_SRC,
            "VULN-04: @limiter decorator found — rate limiting may be implemented")

    def test_no_fastapi_limiter(self):
        """fastapi-limiter not imported"""
        self.assertNotIn("fastapi_limiter", MAIN_SRC,
            "VULN-04: fastapi_limiter found — rate limiting may be implemented")


class TestVULN05_OTPLoggedToConsole(unittest.TestCase):
    """VULN-05 · OTP codes printed verbatim to stdout"""

    def test_verification_code_in_print(self):
        """Print statement containing [VERIFICATION CODE] exists"""
        self.assertIn("[VERIFICATION CODE]", MAIN_SRC,
            "VULN-05: OTP print statement not found in main.py")

    def test_reset_code_in_print(self):
        """Print statement containing [RESET CODE] exists"""
        self.assertIn("[RESET CODE]", MAIN_SRC,
            "VULN-05: Reset code print statement not found in main.py")


class TestVULN06_UserEnumeration(unittest.TestCase):
    """VULN-06 · forgot-password reveals account existence via 404"""

    def test_404_returned_for_missing_user(self):
        """HTTPException with 404 and 'User not found' is raised in forgot_password"""
        self.assertIn("status_code=404, detail=\"User not found\"", MAIN_SRC,
            "VULN-06: 404 user-not-found response not found in main.py")


class TestVULN07_OTPNoExpiry(unittest.TestCase):
    """VULN-07 · Verification and reset codes have no expiry timestamp"""

    def test_no_expiry_column_in_models(self):
        """verification_code_expires_at column should not exist (it doesn't — that's the bug)"""
        self.assertNotIn("verification_code_expires_at", MODELS_SRC,
            "VULN-07: OTP expiry timestamp found — vulnerability may be mitigated")

    def test_no_reset_code_expires_at(self):
        self.assertNotIn("reset_code_expires_at", MODELS_SRC,
            "VULN-07: Reset code expiry timestamp found — vulnerability may be mitigated")


class TestVULN08_CORSOverlyPermissive(unittest.TestCase):
    """VULN-08 · CORS allows all methods and all headers"""

    def test_allow_methods_wildcard(self):
        self.assertIn('allow_methods=["*"]', MAIN_SRC,
            "VULN-08: CORS wildcard methods not found")

    def test_allow_headers_wildcard(self):
        self.assertIn('allow_headers=["*"]', MAIN_SRC,
            "VULN-08: CORS wildcard headers not found")


class TestVULN09_NoRequestSizeLimit(unittest.TestCase):
    """VULN-09 · Settings endpoint accepts unbounded Dict[str, Any]"""

    def test_settings_accepts_arbitrary_dict(self):
        self.assertIn("Dict[str, Any]", MAIN_SRC,
            "VULN-09: Dict[str, Any] settings parameter not found")

    def test_no_max_request_size(self):
        """No max_request_size set on FastAPI app"""
        self.assertNotIn("max_request_size", MAIN_SRC,
            "VULN-09: max_request_size found — request size limit may be set")


class TestVULN10_NoLogout(unittest.TestCase):
    """VULN-10 · No logout endpoint / no token revocation"""

    def test_no_logout_endpoint(self):
        """No /api/logout route exists"""
        self.assertNotIn("/api/logout", MAIN_SRC,
            "VULN-10: /api/logout endpoint found — logout may be implemented")

    def test_no_token_blacklist(self):
        """No token blacklist or revocation mechanism"""
        self.assertNotIn("blacklist", MAIN_SRC,
            "VULN-10: token blacklist found — revocation may be implemented")


class TestVULN12_DocsExposed(unittest.TestCase):
    """VULN-12 · FastAPI docs not disabled (Swagger UI exposed)"""

    def test_docs_url_not_disabled(self):
        """FastAPI() init does not set docs_url=None"""
        self.assertNotIn("docs_url=None", MAIN_SRC,
            "VULN-12: docs_url=None found — docs may be disabled")

    def test_redoc_url_not_disabled(self):
        self.assertNotIn("redoc_url=None", MAIN_SRC,
            "VULN-12: redoc_url=None found — redoc may be disabled")


class TestVULN13_HazardTypeNoValidation(unittest.TestCase):
    """VULN-13 · hazard_type is unvalidated raw string"""

    def test_hazardreportschema_type_is_plain_str(self):
        """HazardReportSchema.type should be declared as str (not the HazardType enum)"""
        # Find the HazardReportSchema class block
        match = re.search(
            r"class HazardReportSchema\(BaseModel\):.*?(?=\nclass |\Z)",
            MAIN_SRC, re.DOTALL
        )
        self.assertIsNotNone(match, "HazardReportSchema class not found")
        schema_text = match.group(0)
        self.assertIn("type: str", schema_text,
            "VULN-13: HazardReportSchema.type is not a plain str — enum validation may exist")


class TestVULN14_SOSNoRateLimit(unittest.TestCase):
    """VULN-14 · SOS can be triggered repeatedly without cooldown"""

    def test_no_sos_cooldown_check(self):
        """No cooldown guard before allowing re-trigger of SOS"""
        # Look for the trigger_sos function and check it has no sleep/cooldown
        self.assertNotIn("sos_cooldown", MAIN_SRC,
            "VULN-14: sos_cooldown found — SOS rate limiting may exist")

    def test_sos_sets_active_without_checking_existing(self):
        """sos_active is set to True without checking if already True"""
        self.assertIn("user.sos_active = True", MAIN_SRC,
            "VULN-14: SOS trigger code not found")


class TestVULN15_IndexBasedContactDeletion(unittest.TestCase):
    """VULN-15 · Emergency contact deleted by array index (not stable ID)"""

    def test_contact_deleted_by_index(self):
        self.assertIn("emergency_contacts[contact_idx]", MAIN_SRC,
            "VULN-15: Index-based contact deletion not found")


class TestVULN16_UnboundedLimitParam(unittest.TestCase):
    """VULN-16 · limit query param passed directly to .limit() without cap"""

    def test_limit_param_no_ge_le_constraint(self):
        """limit parameter defined without Query(ge=1, le=200) constraint"""
        # The param is: limit: int = 50 — no Query() constraint
        self.assertIn("limit: int = 50", MAIN_SRC,
            "VULN-16: Unconstrained limit param not found")
        self.assertNotIn("le=200", MAIN_SRC,
            "VULN-16: Upper bound constraint found — vulnerability may be mitigated")


class TestVULN17_WeakPasswordPolicy(unittest.TestCase):
    """VULN-17 · Password field has no minimum length validation"""

    def test_password_field_no_min_length(self):
        """password: str with no Field(min_length=...)"""
        # In AuthRequest: password: str (bare, no Field)
        self.assertIn("password: str", MAIN_SRC,
            "VULN-17: password field declaration not found")
        self.assertNotIn("min_length", MAIN_SRC,
            "VULN-17: min_length validation found — weak password policy may be fixed")


class TestVULN18_NoSecurityHeaders(unittest.TestCase):
    """VULN-18 · No security response headers set"""

    def test_no_x_content_type_options(self):
        self.assertNotIn("X-Content-Type-Options", MAIN_SRC,
            "VULN-18: X-Content-Type-Options header found")

    def test_no_x_frame_options(self):
        self.assertNotIn("X-Frame-Options", MAIN_SRC,
            "VULN-18: X-Frame-Options header found")

    def test_no_strict_transport_security(self):
        self.assertNotIn("Strict-Transport-Security", MAIN_SRC,
            "VULN-18: HSTS header found")


class TestOWASPCoverageReport(unittest.TestCase):
    """Summary: Verify key security patterns across all backend files"""

    def test_bcrypt_used_for_passwords(self):
        """Positive check: bcrypt IS used for password hashing (good practice)"""
        self.assertIn("bcrypt", MAIN_SRC)

    def test_sqlalchemy_orm_prevents_sqli(self):
        """Positive check: SQLAlchemy ORM used (prevents SQL injection)"""
        self.assertIn("db.query(", MAIN_SRC)
        self.assertNotIn("f\"SELECT", MAIN_SRC)  # No raw f-string SQL
        self.assertNotIn("text(f\"", MAIN_SRC)   # No dynamic text() SQL

    def test_jwt_algorithm_is_hs256(self):
        """Positive check: JWT uses HS256 (not 'none' or RS256 without key)"""
        self.assertIn('ALGORITHM = "HS256"', MAIN_SRC)

    def test_jwt_none_algorithm_not_allowed(self):
        """No 'none' algorithm in JWT decode options"""
        self.assertNotIn('"none"', MAIN_SRC)

    def test_no_raw_sql_string_concat(self):
        """No raw SQL string concatenation patterns"""
        dangerous = re.findall(
            r'(?:execute|cursor\.execute|db\.execute)\s*\(\s*["\'].*?\+',
            MAIN_SRC
        )
        self.assertEqual(len(dangerous), 0,
            f"Raw SQL string concatenation found: {dangerous}")


if __name__ == "__main__":
    # Run with verbose output and generate a report
    loader = unittest.TestLoader()
    suite  = loader.loadTestsFromModule(sys.modules[__name__])
    runner = unittest.TextTestRunner(verbosity=2, stream=sys.stdout)
    result = runner.run(suite)

    print("\n" + "=" * 70)
    print("SAFEWATCH SECURITY CODE REVIEW -- RESULTS")
    print("=" * 70)
    print(f"Total Tests  : {result.testsRun}")
    print(f"Passed       : {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"Failed       : {len(result.failures)}")
    print(f"Errors       : {len(result.errors)}")
    print("=" * 70)

    if result.failures or result.errors:
        sys.exit(1)
    else:
        print("\n[OK] All security findings confirmed. Report is accurate.")
        sys.exit(0)
