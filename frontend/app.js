/* ==========================================
   SAFEWATCH PRODUCTION FRONTEND CONTROLLER
   ========================================== */

document.addEventListener("DOMContentLoaded", () => {
    // ------------------------------------------
    // DYNAMIC GLOBAL STATE REGISTRIES
    // ------------------------------------------
    let currentUser = null;
    let authToken = null; // JWT token for API calls
    let map = null;
    let userMarker = null;
    let circleMarkers = {}; // Peer connection tracking pins
    let hazardCircles = []; // Glowing red threat zone layers
    let safeZoneCircle = null; // Leaflet circle geofence layer

    let currentCoords = { lat: 13.7563, lng: 100.5018 }; // Bangkok Default
    let currentSpeed = 0.0;
    let batteryLevel = 100.0;
    let signalStrength = 92;
    let geolocationWatchId = null;

    // Helper to get auth headers for every API call
    function authHeaders() {
        return {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${authToken}`
        };
    }

    function isValidEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    function isValidPhone(phone) {
        const re = /^\+?[0-9\s\-()]{3,20}$/;
        return re.test(phone);
    }

    // Safety Timer State
    let safetyTimerDuration = 10;
    let safetyTimerRemaining = 0;
    let safetyTimerInterval = null;
    let isSafetyTimerArmed = false;
    let tickingAudioCtx = null;

    // Fake Call Cadence
    let fakeCallTimeoutId = null;
    let phoneCallAudioCtx = null;
    let phoneRingInterval = null;
    let fakeCallTalkingTimer = 0;
    let fakeCallTalkingInterval = null;
    let fakeCallVoiceInterval = null;

    // Media Capture Streams
    let mediaStream = null;
    let mediaRecorder = null;
    let recordedChunks = [];
    let videoStream = null;
    let videoRecorder = null;
    let videoChunks = [];

    // Safety Heatmap Mode
    let isHazardReportingMode = false;

    // Hardware and shake toggle states
    let isShakeTriggerActive = true;
    let isHardwareTriggerActive = true;

    // Active Screen references
    const screens = {
        login: document.getElementById("screen-login"),
        signup: document.getElementById("screen-signup"),
        verify: document.getElementById("screen-verify"),
        forgot: document.getElementById("screen-forgot"),
        reset: document.getElementById("screen-reset"),
        dashboard: document.getElementById("screen-dashboard"),
        profile: document.getElementById("screen-profile"),
        safezone: document.getElementById("screen-safezone"),
        history: document.getElementById("screen-history"),
        settings: document.getElementById("screen-settings"),
        sosActive: document.getElementById("screen-sos-active"),
        timer: document.getElementById("screen-timer"),
        circle: document.getElementById("screen-circle"),
        fakecallConfig: document.getElementById("screen-fake-call-config"),
        fakecallActive: document.getElementById("screen-fake-call-active"),
        fakecallTalking: document.getElementById("screen-fake-call-talking")
    };

    // ------------------------------------------
    // SPA SCREEN ROUTER
    // ------------------------------------------
    function showScreen(screenKey) {
        Object.keys(screens).forEach(key => {
            if (key === screenKey) {
                screens[key].classList.add("active");
            } else {
                screens[key].classList.remove("active");
            }
        });

        // Map refresh fits
        if (screenKey === "dashboard" && map) {
            setTimeout(() => {
                map.invalidateSize();
            }, 150);
        }
    }

    // Connect back-to-hub buttons globally
    document.querySelectorAll(".btn-back-hub").forEach(btn => {
        btn.addEventListener("click", () => {
            showScreen("dashboard");
            syncDashboardMetrics();
        });
    });

    // Secondary card clicks redirection
    document.getElementById("card-safety-timer").addEventListener("click", () => showScreen("timer"));
    document.getElementById("card-emergency-circle").addEventListener("click", () => {
        showScreen("circle");
        syncContactsList();
    });
    document.getElementById("card-fake-call").addEventListener("click", () => showScreen("fakecallConfig"));
    document.getElementById("card-safezone-manager").addEventListener("click", () => {
        showScreen("safezone");
        syncSafeZoneUI();
    });
    document.getElementById("card-travel-history").addEventListener("click", () => {
        showScreen("history");
        renderTravelTimeline();
    });
    document.getElementById("card-master-settings").addEventListener("click", () => showScreen("settings"));
    
    // Header click redirections
    document.getElementById("btn-dashboard-profile").addEventListener("click", () => openProfileGateway());
    document.getElementById("profile-name").addEventListener("click", () => openProfileGateway());

    // ------------------------------------------
    // THEMES & ACCENTS PERSONALIZATION CONTROLS
    // ------------------------------------------
    const btnToggleAmoled = document.getElementById("btn-toggle-amoled");
    const btnToggleShake = document.getElementById("btn-toggle-shake");
    const btnToggleHardware = document.getElementById("btn-toggle-hardware");
    
    // AMOLED Absolute Dark Toggle
    btnToggleAmoled.addEventListener("click", () => {
        const isAmoled = document.body.classList.toggle("amoled-mode");
        btnToggleAmoled.classList.toggle("active", isAmoled);
        sessionStorage.setItem("amoled", isAmoled ? "true" : "false");
        persistSettings({ amoled_mode: isAmoled });
    });

    // Toggle Toggles
    btnToggleShake.addEventListener("click", () => {
        isShakeTriggerActive = !isShakeTriggerActive;
        btnToggleShake.classList.toggle("active", isShakeTriggerActive);
        persistSettings({ shake_to_alert: isShakeTriggerActive });
    });

    btnToggleHardware.addEventListener("click", () => {
        isHardwareTriggerActive = !isHardwareTriggerActive;
        btnToggleHardware.classList.toggle("active", isHardwareTriggerActive);
        persistSettings({ hardware_trigger: isHardwareTriggerActive });
    });

    // Accents Color dots selection
    document.querySelectorAll(".accent-dot").forEach(dot => {
        dot.addEventListener("click", () => {
            document.querySelectorAll(".accent-dot").forEach(d => d.classList.remove("active"));
            dot.classList.add("active");
            
            const accent = dot.getAttribute("data-accent");
            document.body.className = `${accent} ${document.body.classList.contains("amoled-mode") ? "amoled-mode" : ""}`;
            sessionStorage.setItem("accent", accent);
            persistSettings({ accent_color: accent });
        });
    });

    // Persist settings to backend
    async function persistSettings(settings) {
        if (!authToken) return;
        try {
            await fetch("/api/settings", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify(settings)
            });
        } catch (e) { /* silently fail */ }
    }

    // Load theme state from storage
    function loadThemePreferences() {
        const amoled = sessionStorage.getItem("amoled");
        if (amoled === "true") {
            document.body.classList.add("amoled-mode");
            btnToggleAmoled.classList.add("active");
        }
        
        const accent = sessionStorage.getItem("accent") || "accent-blue";
        document.body.classList.remove("accent-blue", "accent-purple", "accent-green");
        document.body.classList.add(accent);
        
        document.querySelectorAll(".accent-dot").forEach(dot => {
            if (dot.getAttribute("data-accent") === accent) {
                dot.classList.add("active");
            } else {
                dot.classList.remove("active");
            }
        });
    }

    // ------------------------------------------
    // TELEMETRY ENGINE & GEOLOCATION SENSORS
    // ------------------------------------------
    function initTelemetry() {
        // Battery status listener
        if (navigator.getBattery) {
            navigator.getBattery().then(bat => {
                updateBatteryState(bat.level * 100);
                bat.addEventListener("levelchange", () => {
                    updateBatteryState(bat.level * 100);
                });
            });
        } else {
            updateBatteryState(88.0);
            setInterval(() => {
                if (batteryLevel > 1) updateBatteryState(batteryLevel - 0.05);
            }, 15000);
        }

        // Simulated signal DBm
        setInterval(() => {
            const delta = Math.floor(Math.random() * 8) - 4;
            signalStrength = Math.max(-110, Math.min(-50, signalStrength + delta));
            // display on telemetry card
            document.getElementById("tel-signal").textContent = signalStrength;
            const pct = Math.floor(((signalStrength + 110) / 60) * 100);
            document.getElementById("bar-signal").style.width = `${Math.max(10, Math.min(100, pct))}%`;
        }, 4000);

        // Geolocation tracking
        if (navigator.geolocation) {
            geolocationWatchId = navigator.geolocation.watchPosition(
                (pos) => {
                    currentCoords.lat = pos.coords.latitude;
                    currentCoords.lng = pos.coords.longitude;
                    currentSpeed = pos.coords.speed !== null ? (pos.coords.speed * 3.6) : (Math.random() * 5.2);
                    updateUIWithTelemetry();
                },
                (err) => {
                    console.warn("Static location watch. Emulating walking simulation vector.");
                    startSimulatedWalker();
                },
                { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
            );
        } else {
            startSimulatedWalker();
        }

        // Sync loops: coordinate transmission + hazard pings + peer pings every 5 seconds
        setInterval(syncTelemetryAndLogs, 5000);
    }

    function updateBatteryState(level) {
        batteryLevel = parseFloat(level.toFixed(1));
        document.getElementById("tel-battery").textContent = batteryLevel;
        document.getElementById("bar-battery").style.width = `${batteryLevel}%`;
    }

    function startSimulatedWalker() {
        // Fallback default coordinate centers
        currentCoords = { lat: 13.7563, lng: 100.5018 };
        updateUIWithTelemetry();
        
        setInterval(() => {
            // Emulate coordinate footsteps
            const deltaLat = (Math.random() - 0.5) * 0.00018;
            const deltaLng = (Math.random() - 0.5) * 0.00018;
            currentCoords.lat += deltaLat;
            currentCoords.lng += deltaLng;
            
            currentSpeed = parseFloat((3.4 + Math.random() * 2.2).toFixed(1));
            updateUIWithTelemetry();
        }, 2000);
    }

    function updateUIWithTelemetry() {
        document.getElementById("tel-speed").textContent = currentSpeed.toFixed(1);
        const speedPct = Math.min(100, (currentSpeed / 20) * 100);
        document.getElementById("bar-speed").style.width = `${speedPct}%`;

        if (map && userMarker) {
            userMarker.setLatLng([currentCoords.lat, currentCoords.lng]);
        }
    }

    async function syncTelemetryAndLogs() {
        if (!currentUser || !authToken) return;

        try {
            const response = await fetch("/api/sync", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({
                    lat: currentCoords.lat,
                    lng: currentCoords.lng,
                    speed: currentSpeed,
                    battery: batteryLevel,
                    signal: signalStrength
                })
            });
            const data = await response.json();
            
            if (response.ok) {
                if (data.last_breath_triggered) {
                    alert("CRITICAL WARNING: Telemetry battery < 5%! Automated Last Breath Broadcast successfully executed.");
                }
                
                if (data.violation_detected) {
                    beep(800, 0.5); // Geofence breach beep
                    alert("SECURITY ALERT: Geofence violation detected! Your coordinates are outside the 500m SafeZone perimeter.");
                    syncSafeZoneUI(); // Refresh violations
                }

                // If SOS is programmatically toggled by safety timer or battery
                if (data.sos_active && !screens.sosActive.classList.contains("active")) {
                    deployDistressScreen();
                }
            }
        } catch (e) {
            console.error("Telemetry sync connection dropped.", e);
        }

        // Pull peer connection states and hazard maps
        syncCircleState();
        pollHazardPings();
    }

    // ------------------------------------------
    // LEAFLET CYBER MAP BUILDER
    // ------------------------------------------
    function initMap() {
        if (typeof L === 'undefined') {
            console.warn("Leaflet library is not loaded. Skipping map initialization.");
            return;
        }
        if (map) {
            console.warn("Map container is already initialized.");
            return;
        }
        map = L.map("live-gps-map", { zoomControl: false }).setView([currentCoords.lat, currentCoords.lng], 15);
        
        L.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", {
            attribution: '&copy; CartoDB',
            subdomains: 'abcd',
            maxZoom: 20
        }).addTo(map);

        // Core green radar divicon pin for operator
        const operatorIcon = L.divIcon({
            className: "operator-marker-html-icon",
            html: `<div style="
                width: 14px;
                height: 14px;
                background-color: #00FF41;
                border: 2px solid white;
                border-radius: 50%;
                box-shadow: 0 0 10px #00FF41;
            "></div>`,
            iconSize: [14, 14],
            iconAnchor: [7, 7]
        });

        userMarker = L.marker([currentCoords.lat, currentCoords.lng], { icon: operatorIcon }).addTo(map);
        userMarker.bindPopup("<b class='font-orbitron'>YOUR TELEMETRY</b><br>Encryption Signal Secure.").openPopup();

        setTimeout(() => {
            map.panTo([currentCoords.lat, currentCoords.lng]);
        }, 500);

        // Map hazard ping reporter click hooks
        map.on("click", (e) => {
            if (isHazardReportingMode) {
                commitHazardReport(e.latlng.lat, e.latlng.lng);
            }
        });
    }

    // ------------------------------------------
    // IDENTITY & SECURITY AUTHENTICATION PORTS
    // ------------------------------------------
    
    // Portal screen swapper buttons
    document.getElementById("to-signup").addEventListener("click", () => showScreen("signup"));
    document.getElementById("to-login").addEventListener("click", () => showScreen("login"));
    document.getElementById("to-forgot-password").addEventListener("click", () => showScreen("forgot"));
    document.getElementById("verify-to-login").addEventListener("click", () => showScreen("login"));
    document.getElementById("forgot-to-login").addEventListener("click", () => showScreen("login"));
    document.getElementById("reset-to-login").addEventListener("click", () => showScreen("login"));

    // Register Signup Form
    document.getElementById("signup-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const email = document.getElementById("signup-email").value;
        const phonePersonal = document.getElementById("signup-phone").value;
        const password = document.getElementById("signup-password").value;
        const name = document.getElementById("signup-name").value;
        const blood = document.getElementById("signup-blood").value;
        const phone = document.getElementById("signup-emergency-phone").value;
        const address = document.getElementById("signup-address").value;

        if (!email && !phonePersonal) {
            alert("Security Protocol: Either email or personal phone number is required.");
            return;
        }

        if (email && !isValidEmail(email)) {
            alert("Invalid Email Format: Please enter a valid email address.");
            return;
        }

        if (phonePersonal && !isValidPhone(phonePersonal)) {
            alert("Invalid Phone Format: Please enter a valid personal phone number.");
            return;
        }

        if (phone && !isValidPhone(phone)) {
            alert("Invalid Emergency Speed Phone Format: Please enter a valid emergency phone number.");
            return;
        }

        try {
            const response = await fetch("/api/auth", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    action: "signup",
                    email: email || null,
                    phone: phonePersonal || null,
                    password, 
                    name, 
                    blood_group: blood, 
                    address, 
                    emergency_phone: phone, 
                    avatar: "avatar-shield"
                })
            });
            const data = await response.json();
            
            if (response.ok) {
                if (data.user && !data.user.is_verified) {
                    alert("Verification code has been sent. Please verify your account.");
                    document.getElementById("verify-identifier").value = email || phonePersonal;
                    showScreen("verify");
                } else {
                    alert("Beacon Profile enrolled successfully! Return to gateway to authenticate.");
                    showScreen("login");
                }
            } else {
                alert(`Gate Enrollment Rejection: ${data.detail}`);
            }
        } catch (err) {
            alert("Security gateway signup timeout.");
        }
    });

    // Entry Authorization Login Form
    document.getElementById("login-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const identifier = document.getElementById("login-email").value;
        const password = document.getElementById("login-password").value;
        const isEmail = identifier.includes("@");

        try {
            const response = await fetch("/api/auth", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    action: "login",
                    email: isEmail ? identifier : null,
                    phone: !isEmail ? identifier : null,
                    password
                })
            });
            const data = await response.json();
            
            if (response.ok) {
                currentUser = data.user;
                authToken = data.access_token;
                sessionStorage.setItem("user", JSON.stringify(currentUser));
                sessionStorage.setItem("token", authToken);
                
                // Initialize modules
                document.getElementById("profile-name").textContent = `OPERATOR: ${currentUser.name.toUpperCase()}`;
                
                try { initMap(); } catch(e) { console.error("Map init failed:", e); }
                try { initTelemetry(); } catch(e) { console.error("Telemetry init failed:", e); }
                try { loadThemePreferences(); } catch(e) { console.error("Theme preferences load failed:", e); }
                try { syncContactsList(); } catch(e) { console.error("Contacts list sync failed:", e); }
                try { syncSafeZoneUI(); } catch(e) { console.error("SafeZone UI sync failed:", e); }
                try { renderTravelTimeline(); } catch(e) { console.error("Travel timeline render failed:", e); }
                try { initFCM(); } catch(e) { console.error("FCM init failed:", e); }

                showScreen("dashboard");
            } else {
                if (data.detail && data.detail.includes("Account not verified")) {
                    alert(data.detail);
                    document.getElementById("verify-identifier").value = identifier;
                    showScreen("verify");
                } else {
                    alert(`Credential Verification Failure: ${data.detail}`);
                }
            }
        } catch (err) {
            alert("Gateway handshake failed. Server offline.");
        }
    });

    // Account Verification Submission
    document.getElementById("verify-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const identifier = document.getElementById("verify-identifier").value;
        const code = document.getElementById("verify-code").value;

        try {
            const response = await fetch("/api/auth/verify", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ identifier, code })
            });
            const data = await response.json();

            if (response.ok) {
                alert("Account verified successfully! Logging you in...");
                currentUser = data.user;
                authToken = data.access_token;
                sessionStorage.setItem("user", JSON.stringify(currentUser));
                sessionStorage.setItem("token", authToken);

                document.getElementById("profile-name").textContent = `OPERATOR: ${currentUser.name.toUpperCase()}`;
                try { initMap(); } catch(e) { console.error("Map init failed:", e); }
                try { initTelemetry(); } catch(e) { console.error("Telemetry init failed:", e); }
                try { loadThemePreferences(); } catch(e) { console.error("Theme preferences load failed:", e); }
                try { syncContactsList(); } catch(e) { console.error("Contacts list sync failed:", e); }
                try { syncSafeZoneUI(); } catch(e) { console.error("SafeZone UI sync failed:", e); }
                try { renderTravelTimeline(); } catch(e) { console.error("Travel timeline render failed:", e); }
                try { initFCM(); } catch(e) { console.error("FCM init failed:", e); }

                showScreen("dashboard");
            } else {
                alert(`Verification Error: ${data.detail}`);
            }
        } catch (err) {
            alert("Handshake verification failed.");
        }
    });

    // Forgot Password Reset Code Request
    document.getElementById("forgot-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const identifier = document.getElementById("forgot-identifier").value;

        try {
            const response = await fetch("/api/auth/forgot-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ identifier })
            });
            const data = await response.json();

            if (response.ok) {
                alert("Credentials Recovery: Verification code sent successfully.");
                document.getElementById("reset-identifier").value = identifier;
                showScreen("reset");
            } else {
                alert(`Recovery Error: ${data.detail}`);
            }
        } catch (err) {
            alert("Forgot password request failed.");
        }
    });

    // Reset Password Submission
    document.getElementById("reset-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const identifier = document.getElementById("reset-identifier").value;
        const code = document.getElementById("reset-code").value;
        const new_password = document.getElementById("reset-new-password").value;

        try {
            const response = await fetch("/api/auth/reset-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ identifier, code, new_password })
            });
            const data = await response.json();

            if (response.ok) {
                alert("Credentials updated successfully. Proceeding to Login Port.");
                showScreen("login");
            } else {
                alert(`Reset Error: ${data.detail}`);
            }
        } catch (err) {
            alert("Reset credentials handshake dropped.");
        }
    });

    // Session Terminating
    document.getElementById("btn-logout").addEventListener("click", () => {
        sessionStorage.clear();
        currentUser = null;
        if (geolocationWatchId) navigator.geolocation.clearWatch(geolocationWatchId);
        location.reload();
    });


    // ------------------------------------------
    // USER PROFILE VIEW PORTAL
    // ------------------------------------------
    let selectedAvatar = "avatar-shield";

    function openProfileGateway() {
        if (!currentUser) return;
        
        document.getElementById("profile-name-input").value = currentUser.name;
        document.getElementById("profile-phone-input").value = currentUser.emergency_phone || "";
        document.getElementById("profile-email-input").value = currentUser.email;
        document.getElementById("profile-blood-input").value = currentUser.blood_group || "O+";
        document.getElementById("profile-address-input").value = currentUser.address || "";
        
        selectedAvatar = currentUser.avatar || "avatar-shield";
        highlightActiveAvatar(selectedAvatar);

        showScreen("profile");
    }

    // Avatar selections badges
    const avatarBadges = document.querySelectorAll(".avatar-badge");
    avatarBadges.forEach(badge => {
        badge.addEventListener("click", () => {
            avatarBadges.forEach(b => b.classList.remove("active"));
            badge.classList.add("active");
            selectedAvatar = badge.getAttribute("data-id");
        });
    });

    function highlightActiveAvatar(avatarId) {
        avatarBadges.forEach(b => {
            if (b.getAttribute("data-id") === avatarId) {
                b.classList.add("active");
            } else {
                b.classList.remove("active");
            }
        });
        
        const avatarMap = { "avatar-shield": "🛡️", "avatar-eye": "👁️", "avatar-falcon": "🦅", "avatar-ghost": "👻" };
        document.getElementById("btn-dashboard-profile").textContent = avatarMap[avatarId] || "👤";
    }

    // Profile updates submission
    document.getElementById("profile-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        
        const name = document.getElementById("profile-name-input").value;
        const phone = document.getElementById("profile-phone-input").value;
        const blood = document.getElementById("profile-blood-input").value;
        const address = document.getElementById("profile-address-input").value;

        if (phone && !isValidPhone(phone)) {
            alert("Invalid Emergency Speed Phone Format: Please enter a valid emergency phone number.");
            return;
        }

        try {
            const response = await fetch("/api/register", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({
                    name, emergency_phone: phone, blood_group: blood, address, avatar: selectedAvatar
                })
            });
            const data = await response.json();
            
            if (response.ok) {
                currentUser = data.user;
                sessionStorage.setItem("user", JSON.stringify(currentUser));
                document.getElementById("profile-name").textContent = `OPERATOR: ${currentUser.name.toUpperCase()}`;
                highlightActiveAvatar(currentUser.avatar);
                alert("Operational profile sync saved.");
                showScreen("dashboard");
            } else {
                alert("Failed to sync profile telemetry data.");
            }
        } catch (err) {
            alert("Profile telemetry transaction timed out.");
        }
    });

    // Evidence Vault feature removed per user request

    // ------------------------------------------
    // INTERACTIVE SAFEZONE GEOFENCING
    // ------------------------------------------
    const btnZoneToggle = document.getElementById("btn-safezone-toggle");
    const btnLockNode = document.getElementById("btn-lock-safezone-node");

    btnLockNode.addEventListener("click", () => {
        document.getElementById("safezone-lat").value = currentCoords.lat.toFixed(6);
        document.getElementById("safezone-lng").value = currentCoords.lng.toFixed(6);
        commitSafeZone(true);
    });

    btnZoneToggle.addEventListener("click", () => {
        const isActive = btnZoneToggle.classList.contains("btn-purple");
        commitSafeZone(!isActive);
    });

    async function commitSafeZone(active) {
        if (!currentUser || !authToken) return;
        
        let lat = parseFloat(document.getElementById("safezone-lat").value);
        let lng = parseFloat(document.getElementById("safezone-lng").value);
        
        if (isNaN(lat)) {
            lat = currentCoords.lat;
            lng = currentCoords.lng;
        }

        try {
            const response = await fetch("/api/safezone", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ lat, lng, radius: 500.0, active })
            });
            
            if (response.ok) {
                alert(active ? "SafeZone geofence activated." : "SafeZone geofence deactivated.");
                syncSafeZoneUI();
            }
        } catch (e) {
            alert("Safezone configuration sync failure.");
        }
    }

    async function syncSafeZoneUI() {
        if (!currentUser || !authToken) return;

        try {
            const response = await fetch("/api/fetch-contacts", {
                headers: { "Authorization": `Bearer ${authToken}` }
            });
            const data = await response.json();
            
            if (response.ok) {
                // Set form values
                const zone = data.safe_zone || {};
                
                if (zone.active && zone.lat !== 0.0) {
                    btnZoneToggle.className = "cyber-btn btn-purple font-orbitron btn-small";
                    btnZoneToggle.textContent = "DEACTIVATE GEOFENCE";
                    document.getElementById("safezone-lat").value = zone.lat.toFixed(6);
                    document.getElementById("safezone-lng").value = zone.lng.toFixed(6);
                    
                    document.getElementById("safezone-tool-status").textContent = "GEOFENCE ARMED (500M)";
                    document.getElementById("safezone-tool-status").className = "tool-status font-orbitron text-purple active-glow";

                    // Map rendering circular SafeZone overlay
                    if (map) {
                        if (safeZoneCircle) map.removeLayer(safeZoneCircle);
                        safeZoneCircle = L.circle([zone.lat, zone.lng], {
                            radius: 500,
                            color: '#BD00FF',
                            fillColor: '#BD00FF',
                            fillOpacity: 0.12,
                            weight: 2
                        }).addTo(map);
                    }
                } else {
                    btnZoneToggle.className = "cyber-btn btn-blue font-orbitron btn-small";
                    btnZoneToggle.textContent = "ACTIVATE GEOFENCE";
                    
                    document.getElementById("safezone-tool-status").textContent = "GEOFENCE INACTIVE";
                    document.getElementById("safezone-tool-status").className = "tool-status font-orbitron text-purple";

                    if (map && safeZoneCircle) {
                        map.removeLayer(safeZoneCircle);
                        safeZoneCircle = null;
                    }
                }

                // Render violations list
                const violationsDiv = document.getElementById("safezone-breach-container");
                violationsDiv.innerHTML = "";
                
                const breaches = data.safe_zone_violations || [];
                if (breaches.length === 0) {
                    violationsDiv.innerHTML = `<div class="empty-state">SafeZone boundary intact. Zero breach alerts logged.</div>`;
                    return;
                }

                breaches.forEach(item => {
                    const row = document.createElement("div");
                    row.className = "member-item sos-red";
                    row.innerHTML = `
                        <div class="member-info">
                            <span class="member-name">🚨 GEOFENCE BOUNDARY EXIT BREACH</span>
                            <span class="member-meta">Distance: ${item.distance.toFixed(1)}m • Lat: ${item.lat.toFixed(5)}, Lng: ${item.lng.toFixed(5)}</span>
                        </div>
                        <div class="action-row">
                            <span class="blood-badge font-orbitron">${item.timestamp.substring(11, 19)}</span>
                        </div>
                    `;
                    violationsDiv.appendChild(row);
                });
            }
        } catch (e) {
            console.error("SafeZone sync issue");
        }
    }

    // ------------------------------------------
    // DYNAMIC TRAVEL TIMELINE HISTORY
    // ------------------------------------------
    async function renderTravelTimeline() {
        if (!currentUser || !authToken) return;
        
        try {
            const response = await fetch("/api/location-history", {
                headers: { "Authorization": `Bearer ${authToken}` }
            });
            const data = await response.json();
            
            if (response.ok) {
                const box = document.getElementById("vertical-timeline-box");
                box.innerHTML = "";
                
                const history = data.location_history || [];
                document.getElementById("history-tool-status").textContent = `${history.length} LOCATION LOGS`;

                if (history.length === 0) {
                    box.innerHTML = `<div class="empty-state">No historical location logs registered. Initialize sync to store footprints.</div>`;
                    return;
                }

                // Render chronological vertical history elements (reverse order)
                history.slice().reverse().forEach(node => {
                    const item = document.createElement("div");
                    item.className = "timeline-node";
                    
                    const score = node.safety_score || 98;
                    const scoreClass = score >= 80 ? "score-safe" : "score-warn";
                    
                    item.innerHTML = `
                        <div class="timeline-header-row">
                            <span class="timeline-time font-orbitron">${node.timestamp.replace('T', ' ').substring(0, 19)}</span>
                            <span class="timeline-score ${scoreClass} font-orbitron">SAFETY: ${score}%</span>
                        </div>
                        <div class="timeline-body-grid">
                            <span>Coords: <b>${node.lat.toFixed(5)}, ${node.lng.toFixed(5)}</b></span>
                            <span>Speed: <b>${node.speed.toFixed(1)} km/h</b></span>
                            <span>Signal: <b>${node.signal} dBm</b></span>
                        </div>
                    `;
                    box.appendChild(item);
                });
            }
        } catch (e) {
            console.error("Travel logs timeline loading dropped.");
        }
    }

    // ------------------------------------------
    // SHAKE TO SOS DETECTORS & KEYBOARD SHORTCUTS
    // ------------------------------------------
    
    // Keyboard combo interception for hardware triggers simulation: Shift + S + S
    let keySequence = [];
    document.addEventListener("keydown", (e) => {
        if (!isHardwareTriggerActive) return;
        
        // Push keys
        keySequence.push(e.key.toLowerCase());
        if (keySequence.length > 5) keySequence.shift();
        
        // Match sequence: Shift, s, s
        const isTrigger = keySequence.slice(-3).join("") === "s-s" && e.shiftKey;
        if (isTrigger) {
            keySequence = []; // reset
            deployDistressScreen();
            triggerSOSBroadcast();
        }
    });

    // Accelerometer simulated shake dispatch button
    const shakeBtn = document.getElementById("btn-shake-emulate");
    shakeBtn.addEventListener("click", () => {
        if (!isShakeTriggerActive) return;
        deployDistressScreen();
        triggerSOSBroadcast();
    });

    // Mobile Motion Sensor Shake Hooks
    if (window.DeviceMotionEvent) {
        let lastX, lastY, lastZ;
        let moveCounter = 0;
        
        window.addEventListener('devicemotion', (e) => {
            if (!isShakeTriggerActive) return;
            
            const acc = e.accelerationIncludingGravity;
            if (!acc || acc.x === null) return;
            
            let deltaX = Math.abs(acc.x - lastX);
            let deltaY = Math.abs(acc.y - lastY);
            let deltaZ = Math.abs(acc.z - lastZ);
            
            if ((deltaX + deltaY + deltaZ) > 30) { // Shake sensitivity limit
                moveCounter++;
                if (moveCounter > 5) { // continuous motions
                    moveCounter = 0;
                    deployDistressScreen();
                    triggerSOSBroadcast();
                }
            } else {
                moveCounter = Math.max(0, moveCounter - 1);
            }
            
            lastX = acc.x;
            lastY = acc.y;
            lastZ = acc.z;
        });
    }

    // ------------------------------------------
    // PEER CONTACTS HANDSHAKE MANAGER & SPEED DIALS
    // ------------------------------------------
    const directContactForm = document.getElementById("direct-contact-form");
    
    // Add offline manual Direct Dial contact details
    directContactForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        
        const name = document.getElementById("direct-name").value;
        const relationship = document.getElementById("direct-relation").value;
        const phone = document.getElementById("direct-phone").value;

        if (phone && !isValidPhone(phone)) {
            alert("Invalid Phone Format: Please enter a valid phone number.");
            return;
        }

        try {
            const response = await fetch("/api/contacts?action=add_direct", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ name, relationship, phone })
            });
            
            if (response.ok) {
                alert("Offline direct contact registered successfully.");
                document.getElementById("direct-name").value = "";
                document.getElementById("direct-relation").value = "";
                document.getElementById("direct-phone").value = "";
                syncContactsList();
            }
        } catch (err) {
            console.error("Contacts post failed.");
        }
    });

    // Accept invite swappers
    const btnTabDirect = document.getElementById("tab-direct-dials");
    const contentDirect = document.getElementById("tab-content-direct-dials");

    btnTabDirect.addEventListener("click", () => {
        btnTabDirect.classList.add("active");
        document.getElementById("tab-my-circle").classList.remove("active");
        document.getElementById("tab-invites").classList.remove("active");
        
        contentDirect.classList.add("active");
        document.getElementById("tab-content-circle").classList.remove("active");
        document.getElementById("tab-content-invites").classList.remove("active");
    });

    async function syncContactsList() {
        if (!currentUser || !authToken) return;
        
        try {
            const response = await fetch("/api/fetch-contacts", {
                headers: { "Authorization": `Bearer ${authToken}` }
            });
            const data = await response.json();
            
            if (response.ok) {
                // Update direct contact listings
                const listDiv = document.getElementById("circle-direct-list");
                listDiv.innerHTML = "";
                
                const direct = data.emergency_contacts || [];
                if (direct.length === 0) {
                    listDiv.innerHTML = `<div class="empty-state">No direct speed dials listed. Register a profile above.</div>`;
                } else {
                    direct.forEach((item, index) => {
                        const row = document.createElement("div");
                        row.className = "member-item";
                        row.innerHTML = `
                            <div class="member-info">
                                <span class="member-name">${item.name} (${item.relationship})</span>
                                <span class="member-meta">${item.phone}</span>
                            </div>
                            <div class="action-row">
                                <button class="btn-pill btn-pill-red font-orbitron remove-direct-btn" data-idx="${index}">REMOVE</button>
                            </div>
                        `;
                        listDiv.appendChild(row);
                    });

                    // Direct speed dial deletion triggers
                    document.querySelectorAll(".remove-direct-btn").forEach(btn => {
                        btn.addEventListener("click", () => removeDirectContact(parseInt(btn.getAttribute("data-idx"))));
                    });
                }
            }
        } catch (e) {
            console.error("Direct dials sync timed out.");
        }
    }

    async function removeDirectContact(idx) {
        try {
            const response = await fetch("/api/contacts?action=remove_direct", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ index: idx })
            });
            if (response.ok) {
                syncContactsList();
            }
        } catch (e) {
            console.error("Removal failure");
        }
    }

    // ------------------------------------------
    // COMMUNITY MAP HAZARDS REPORT ENGINE
    // ------------------------------------------
    const reportHazardBtn = document.getElementById("btn-add-map-hazard");
    reportHazardBtn.addEventListener("click", () => {
        isHazardReportingMode = !isHazardReportingMode;
        if (isHazardReportingMode) {
            reportHazardBtn.className = "cyber-btn btn-purple font-orbitron btn-small";
            reportHazardBtn.textContent = "DOUBLE TAP ON MAP TO LOG HAZARD";
            map.getContainer().style.cursor = "crosshair";
        } else {
            reportHazardBtn.className = "cyber-btn-outline font-orbitron btn-small btn-purple-glow";
            reportHazardBtn.textContent = "REPORT LOCAL HAZARD";
            map.getContainer().style.cursor = "";
        }
    });

    async function commitHazardReport(lat, lng) {
        isHazardReportingMode = false;
        reportHazardBtn.className = "cyber-btn-outline font-orbitron btn-small btn-purple-glow";
        reportHazardBtn.textContent = "REPORT LOCAL HAZARD";
        map.getContainer().style.cursor = "";

        const types = ["Poor Lighting", "Suspicious Activity", "Aggressive Crowd", "Obstruction"];
        const selectType = prompt("Select Hazard Ping Type:\n1. Poor Lighting\n2. Suspicious Activity\n3. Aggressive Crowd\n4. Obstruction", "1");
        
        let chosenType = types[0];
        if (selectType === "2") chosenType = types[1];
        else if (selectType === "3") chosenType = types[2];
        else if (selectType === "4") chosenType = types[3];

        try {
            const response = await fetch("/api/hazard", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ lat, lng, type: chosenType })
            });
            
            if (response.ok) {
                alert(`Threat Zone registered successfully. Collaborative hazard ping created: "${chosenType}"`);
                pollHazardPings();
            }
        } catch (e) {
            console.error("Hazard post timed out.");
        }
    }

    async function pollHazardPings() {
        if (!map) return;
        
        try {
            const response = await fetch("/api/hazard");
            const data = await response.json();
            
            if (response.ok) {
                // Clear old circles
                hazardCircles.forEach(c => map.removeLayer(c));
                hazardCircles = [];

                const pings = data.hazard_pings || [];
                pings.forEach(ping => {
                    const circle = L.circle([ping.lat, ping.lng], {
                        radius: 80, // 80 meter danger radius
                        color: "#FF0000",
                        fillColor: "#FF0000",
                        fillOpacity: 0.2,
                        weight: 1
                    }).addTo(map);

                    circle.bindPopup(`<b style="color:var(--alert-red);font-family:var(--font-headings);">${ping.type.toUpperCase()}</b><br>Reporter: ${ping.reporter}<br>Signal: high threat`);
                    hazardCircles.push(circle);
                });
            }
        } catch (e) {
            console.error("Hazard polling timed out.");
        }
    }

    // ------------------------------------------
    // ATOMIC PANIC PURGE MECHANICS
    // ------------------------------------------
    const btnPanicWipe = document.getElementById("btn-panic-wipe");
    btnPanicWipe.addEventListener("click", async () => {
        const verify = confirm("WARNING: You are executing the Atomic panic purge. This will irreversibly zero-out your travel history logs, emergency direct dials, coordinates violations, and evidence vaults instantly. Proceed?");
        if (!verify) return;
        
        try {
            const response = await fetch("/api/wipe", {
                method: "POST",
                headers: { "Authorization": `Bearer ${authToken}` }
            });
            
            if (response.ok) {
                alert("PANIC PURGE ENFORCED: Local tracking registries successfully zeroed out.");
                
                // Refresh local UI states
                syncSafeZoneUI();
                renderTravelTimeline();
                syncContactsList();
                
                showScreen("dashboard");
            }
        } catch (e) {
            alert("Panic wipe command transaction dropped.");
        }
    });

    // ------------------------------------------
    // DB INSPECTOR: Live system state viewer
    // ------------------------------------------
    const btnRefreshInspector = document.getElementById("btn-refresh-inspector");
    const rawInspectorBox = document.getElementById("raw-db-inspector");

    btnRefreshInspector.addEventListener("click", async () => {
        if (!authToken) { rawInspectorBox.textContent = "[AUTH] Log in first."; return; }
        rawInspectorBox.textContent = "Loading live telemetry...";

        try {
            const [contactsRes, historyRes, sosRes] = await Promise.all([
                fetch("/api/fetch-contacts", { headers: { "Authorization": `Bearer ${authToken}` } }),
                fetch("/api/location-history", { headers: { "Authorization": `Bearer ${authToken}` } }),
                fetch("/api/sos-history", { headers: { "Authorization": `Bearer ${authToken}` } })
            ]);

            const contacts = await contactsRes.json();
            const history = await historyRes.json();
            const sos = await sosRes.json();

            const report = {
                operator: currentUser ? { name: currentUser.name, email: currentUser.email, blood_group: currentUser.blood_group } : null,
                circle_connected: (contacts.circle || []).length,
                emergency_contacts: (contacts.emergency_contacts || []).length,
                pending_invites: (contacts.invites_received || []).length,
                safe_zone: contacts.safe_zone || {},
                safe_zone_violations: (contacts.safe_zone_violations || []).length,
                location_logs: (history.location_history || []).length,
                latest_location: (history.location_history || [])[0] || null,
                sos_events: (sos.sos_history || []).length,
                latest_sos: (sos.sos_history || [])[0] || null,
                current_telemetry: {
                    lat: currentCoords.lat.toFixed(6),
                    lng: currentCoords.lng.toFixed(6),
                    speed: currentSpeed.toFixed(1) + " km/h",
                    battery: batteryLevel + "%",
                    signal: signalStrength + " dBm"
                }
            };

            rawInspectorBox.textContent = JSON.stringify(report, null, 2);
        } catch (e) {
            rawInspectorBox.textContent = `[ERROR] Failed to load: ${e.message}`;
        }
    });

    // ------------------------------------------
    // SOS BUTTON ON DASHBOARD — Trigger distress
    // ------------------------------------------
    document.getElementById("btn-sos-trigger").addEventListener("click", () => {
        deployDistressScreen();
        triggerSOSBroadcast();
    });

    // ------------------------------------------
    // ADVANCED EMERGENCY FULLSCREEN PULSING SOS
    // ------------------------------------------
    const textDistressLat = document.getElementById("distress-lat");
    const textDistressLng = document.getElementById("distress-lng");
    const textSmsPreview = document.getElementById("autogen-sms-preview");
    const inputSosDisarm = document.getElementById("sos-disarm-pin");
    const btnSosDisarm = document.getElementById("btn-sos-disarm");

    btnSosDisarm.addEventListener("click", () => {
        const pin = inputSosDisarm.value;
        if (pin === "1234" || pin.length > 2) {
            cancelDistressSOS();
        } else {
            alert("Disarm security gate validation failure.");
            beep(300, 0.4);
        }
    });

    function deployDistressScreen() {
        textDistressLat.textContent = currentCoords.lat.toFixed(6);
        textDistressLng.textContent = currentCoords.lng.toFixed(6);
        
        const operatorName = currentUser ? currentUser.name : "SAFEWATCH Operator";
        textSmsPreview.value = `CRITICAL: ${operatorName.toUpperCase()} is in high danger. Live coordinates trace: https://maps.google.com/?q=${currentCoords.lat.toFixed(6)},${currentCoords.lng.toFixed(6)}`;
        
        inputSosDisarm.value = "";
        showScreen("sosActive");
    }

    async function cancelDistressSOS() {
        if (!currentUser || !authToken) return;
        
        try {
            const response = await fetch("/api/cancel-sos", {
                method: "POST",
                headers: { "Authorization": `Bearer ${authToken}` }
            });
            
            if (response.ok) {
                showScreen("dashboard");
                syncDashboardMetrics();
                alert("Distress alert terminated successfully.");
            }
        } catch (e) {
            console.error("SOS cancellation gateway failure.");
        }
    }

    function syncDashboardMetrics() {
        // Keeps card tool statuses synchronized in real-time
        if (!currentUser || !authToken) return;
        fetch("/api/fetch-contacts", {
            headers: { "Authorization": `Bearer ${authToken}` }
        })
            .then(res => res.json())
            .then(data => {
                document.getElementById("circle-tool-status").textContent = `${(data.circle || []).length} CONNECTED NODES`;
                document.getElementById("history-tool-status").textContent = `${(data.location_history || []).length} LOCATION LOGS`;
            });
    }

    // ------------------------------------------
    // RETRACTED HOOKS ON PRE-EXISTING SYSTEMS
    // ------------------------------------------
    
    // Retro alarm beep oscillator helper
    function beep(freq, duration, delay = 0) {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            
            osc.frequency.setValueAtTime(freq, ctx.currentTime + delay);
            gain.gain.setValueAtTime(0, ctx.currentTime + delay);
            gain.gain.linearRampToValueAtTime(0.3, ctx.currentTime + delay + 0.05);
            gain.gain.setValueAtTime(0.3, ctx.currentTime + delay + duration - 0.05);
            gain.gain.linearRampToValueAtTime(0, ctx.currentTime + delay + duration);
            
            osc.connect(gain);
            gain.connect(ctx.destination);
            
            osc.start(ctx.currentTime + delay);
            osc.stop(ctx.currentTime + delay + duration);
        } catch (err) {}
    }

    // Dead man switch arm countdown triggers
    const timerPresets = document.querySelectorAll(".preset-btn");
    const customTimerInput = document.getElementById("custom-timer-input");
    const btnTimerArm = document.getElementById("btn-timer-arm");
    const btnTimerDisarm = document.getElementById("btn-timer-disarm");
    const timerConfigPanel = document.getElementById("timer-config-panel");
    const timerActivePanel = document.getElementById("timer-active-panel");
    const labelCountdown = document.getElementById("timer-countdown-val");
    const labelSubStatus = document.getElementById("timer-sub-status");
    const svgProgress = document.getElementById("radial-progress");
    const inputDisarm = document.getElementById("timer-disarm-password");

    timerPresets.forEach(btn => {
        btn.addEventListener("click", () => {
            timerPresets.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            safetyTimerDuration = parseInt(btn.getAttribute("data-seconds"));
            customTimerInput.value = "";
            updateTimerDisplay(safetyTimerDuration);
        });
    });

    customTimerInput.addEventListener("input", () => {
        timerPresets.forEach(b => b.classList.remove("active"));
        const val = parseInt(customTimerInput.value);
        if (!isNaN(val) && val >= 5) {
            safetyTimerDuration = val;
            updateTimerDisplay(safetyTimerDuration);
        }
    });

    function updateTimerDisplay(totalSeconds) {
        const mins = Math.floor(totalSeconds / 60);
        const secs = totalSeconds % 60;
        labelCountdown.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }

    btnTimerArm.addEventListener("click", () => {
        if (isSafetyTimerArmed) return;

        isSafetyTimerArmed = true;
        safetyTimerRemaining = safetyTimerDuration;
        
        timerConfigPanel.classList.add("hidden");
        timerActivePanel.classList.remove("hidden");
        labelSubStatus.textContent = "MONITORING ARMED";
        labelSubStatus.classList.remove("text-blue");
        labelSubStatus.classList.add("neon-text-red");
        inputDisarm.value = "";

        updateTimerDisplay(safetyTimerRemaining);
        updateRadialProgress(1);

        tickingAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
        document.getElementById("timer-tool-status").textContent = "STATUS: ACTIVE MONITORING";
        document.getElementById("timer-tool-status").className = "tool-status font-orbitron text-blue neon-text-red pulse-fast";

        safetyTimerInterval = setInterval(() => {
            safetyTimerRemaining--;
            updateTimerDisplay(safetyTimerRemaining);
            updateRadialProgress(safetyTimerRemaining / safetyTimerDuration);

            // Heartbeat ticking sound
            playTickSound();

            if (safetyTimerRemaining <= 5) playBuzzerSound();

            if (safetyTimerRemaining <= 0) {
                clearInterval(safetyTimerInterval);
                executeTimerSOS();
            }
        }, 1000);
    });

    btnTimerDisarm.addEventListener("click", () => {
        const pin = inputDisarm.value;
        if (!pin) {
            alert("Disarm security gate validation PIN is required.");
            return;
        }
        
        if (pin === "1234" || pin.length > 2) {
            disarmSafetyTimer();
        } else {
            alert("Disarm validation rejected.");
            beep(300, 0.4);
        }
    });

    function updateRadialProgress(fraction) {
        const offset = 282.7 * (1 - fraction);
        svgProgress.style.strokeDashoffset = offset;
        
        if (fraction <= 0.2) svgProgress.style.stroke = "var(--alert-red)";
        else if (fraction <= 0.5) svgProgress.style.stroke = "#BD00FF";
        else svgProgress.style.stroke = "var(--accent-glow)";
    }

    function playTickSound() {
        try {
            if (tickingAudioCtx.state === 'suspended') tickingAudioCtx.resume();
            const osc = tickingAudioCtx.createOscillator();
            const gain = tickingAudioCtx.createGain();
            osc.frequency.setValueAtTime(600, tickingAudioCtx.currentTime);
            gain.gain.setValueAtTime(0, tickingAudioCtx.currentTime);
            gain.gain.linearRampToValueAtTime(0.08, tickingAudioCtx.currentTime + 0.002);
            gain.gain.exponentialRampToValueAtTime(0.0001, tickingAudioCtx.currentTime + 0.08);
            osc.connect(gain);
            gain.connect(tickingAudioCtx.destination);
            osc.start();
            osc.stop(tickingAudioCtx.currentTime + 0.1);
        } catch (e) {}
    }

    function playBuzzerSound() {
        try {
            if (tickingAudioCtx.state === 'suspended') tickingAudioCtx.resume();
            const osc = tickingAudioCtx.createOscillator();
            const gain = tickingAudioCtx.createGain();
            osc.frequency.setValueAtTime(950, tickingAudioCtx.currentTime);
            gain.gain.setValueAtTime(0, tickingAudioCtx.currentTime);
            gain.gain.linearRampToValueAtTime(0.3, tickingAudioCtx.currentTime + 0.05);
            gain.gain.setValueAtTime(0.3, tickingAudioCtx.currentTime + 0.25);
            gain.gain.linearRampToValueAtTime(0.0001, tickingAudioCtx.currentTime + 0.3);
            osc.connect(gain);
            gain.connect(tickingAudioCtx.destination);
            osc.start();
            osc.stop(tickingAudioCtx.currentTime + 0.35);
        } catch (e) {}
    }

    function disarmSafetyTimer() {
        clearInterval(safetyTimerInterval);
        isSafetyTimerArmed = false;
        
        timerActivePanel.classList.add("hidden");
        timerConfigPanel.classList.remove("hidden");
        labelSubStatus.textContent = "STANDBY";
        labelSubStatus.classList.remove("neon-text-red");
        labelSubStatus.classList.add("text-blue");
        
        updateTimerDisplay(safetyTimerDuration);
        svgProgress.style.strokeDashoffset = 0;
        svgProgress.style.stroke = "var(--accent-glow)";
        
        document.getElementById("timer-tool-status").textContent = "STATUS: MONITOR STANDBY";
        document.getElementById("timer-tool-status").className = "tool-status font-orbitron text-blue";

        if (tickingAudioCtx) {
            tickingAudioCtx.close();
            tickingAudioCtx = null;
        }

        alert("Dead Man's Switch disarmed. Dispatch deactivated.");
    }

    function executeTimerSOS() {
        disarmSafetyTimer();
        deployDistressScreen();
        triggerSOSBroadcast();
    }

    // SOS API Post helper
    async function triggerSOSBroadcast() {
        if (!currentUser || !authToken) return;
        try {
            const response = await fetch("/api/sos/trigger", {
                method: "POST",
                headers: { "Authorization": `Bearer ${authToken}` }
            });
            if (response.ok) {
                beep(880, 0.4);
                beep(880, 0.4, 0.5);
            }
        } catch (e) {
            console.error("SOS broadcast drop.");
        }
    }

    // Peer invitations forms
    document.getElementById("invite-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const target = document.getElementById("invite-phone").value;
        const msg = document.getElementById("invite-status-msg");

        if (target && !isValidPhone(target)) {
            alert("Invalid Phone Format: Please enter a valid phone number.");
            return;
        }

        msg.className = "status-msg-banner";
        msg.textContent = "Transmitting Peer Invitation Link...";

        try {
            const response = await fetch("/api/invite", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ phone: target })
            });
            const data = await response.json();
            
            if (response.ok) {
                msg.className = "status-msg-banner success";
                msg.textContent = "Cyber handshake request successfully transmitted.";
                document.getElementById("invite-phone").value = "";
                syncCircleState();
            } else {
                msg.className = "status-msg-banner error";
                msg.textContent = data.detail;
            }
        } catch (err) {
            msg.className = "status-msg-banner error";
            msg.textContent = "Invitation transmission dropped.";
        }
    });

    const btnTabCircle = document.getElementById("tab-my-circle");
    const btnTabInvites = document.getElementById("tab-invites");
    
    btnTabCircle.addEventListener("click", () => {
        btnTabCircle.classList.add("active");
        btnTabInvites.classList.remove("active");
        btnTabDirect.classList.remove("active");
        
        document.getElementById("tab-content-circle").classList.add("active");
        document.getElementById("tab-content-invites").classList.remove("active");
        contentDirect.classList.remove("active");
    });

    btnTabInvites.addEventListener("click", () => {
        btnTabInvites.classList.add("active");
        btnTabCircle.classList.remove("active");
        btnTabDirect.classList.remove("active");
        
        document.getElementById("tab-content-invites").classList.add("active");
        document.getElementById("tab-content-circle").classList.remove("active");
        contentDirect.classList.remove("active");
    });

    async function syncCircleState() {
        if (!currentUser || !authToken) return;
        try {
            const res = await fetch("/api/fetch-contacts", {
                headers: { "Authorization": `Bearer ${authToken}` }
            });
            const data = await res.json();
            
            if (res.ok) {
                renderCircleMembers(data.circle);
                renderCircleInvites(data.invites_received);
                document.getElementById("circle-tool-status").textContent = `${data.circle.length} CONNECTED NODES`;
                document.getElementById("invites-count").textContent = data.invites_received.length;
            }
        } catch (e) {
            console.error("Circle sync timeout");
        }
    }

    function renderCircleMembers(members) {
        const listDiv = document.getElementById("circle-members-list");
        listDiv.innerHTML = "";

        if (members.length === 0) {
            listDiv.innerHTML = `<div class="empty-state">No secure handshakes established. Transmit a beacon link.</div>`;
            return;
        }

        let currentPhones = {};
        members.forEach(member => {
            currentPhones[member.phone] = true;
            const isSOS = member.sos_active;
            const item = document.createElement("div");
            item.className = `member-item ${isSOS ? 'sos-red' : ''}`;
            
            item.innerHTML = `
                <div class="member-info">
                    <span class="member-name">${member.name} ${isSOS ? '🔥 [SOS DISPATCH]' : ''}</span>
                    <span class="member-meta">
                        <span class="blood-badge font-orbitron">${member.blood_group}</span>
                        ${member.phone} • Bat: ${member.location.battery}%
                    </span>
                </div>
                <div class="action-row">
                    <button class="btn-pill btn-pill-green font-orbitron locate-peer-btn" data-lat="${member.location.lat}" data-lng="${member.location.lng}">MAP</button>
                </div>
            `;
            listDiv.appendChild(item);

            updatePeerMapMarker(member);
        });

        // Clear missing members
        Object.keys(circleMarkers).forEach(phone => {
            if (!currentPhones[phone]) {
                map.removeLayer(circleMarkers[phone]);
                delete circleMarkers[phone];
            }
        });

        document.querySelectorAll(".locate-peer-btn").forEach(btn => {
            btn.addEventListener("click", () => {
                const lat = parseFloat(btn.getAttribute("data-lat"));
                const lng = parseFloat(btn.getAttribute("data-lng"));
                if (map && lat !== 0.0) {
                    map.setView([lat, lng], 16);
                    showScreen("dashboard");
                } else {
                    alert("Node has not synchronized telemetry yet.");
                }
            });
        });
    }

    function updatePeerMapMarker(member) {
        if (!map) return;
        const loc = member.location;
        if (loc.lat === 0.0 || loc.lng === 0.0) return;

        const phone = member.phone;
        const isSOS = member.sos_active;
        const pinColor = isSOS ? "var(--alert-red)" : "#BD00FF";
        
        const peerHtml = `<div style="
            width: 14px;
            height: 14px;
            background-color: ${pinColor};
            border: 2px solid white;
            border-radius: 50%;
            box-shadow: 0 0 10px ${pinColor};
            ${isSOS ? 'animation: pulseSOSFast 0.8s infinite;' : ''}
        "></div>`;

        const peerIcon = L.divIcon({
            className: `peer-divicon-${phone.replace(/[^a-zA-Z0-9]/g, '')}`,
            html: peerHtml,
            iconSize: [14, 14],
            iconAnchor: [7, 7]
        });

        if (circleMarkers[phone]) {
            circleMarkers[phone].setLatLng([loc.lat, loc.lng]);
            circleMarkers[phone].setIcon(peerIcon);
        } else {
            circleMarkers[phone] = L.marker([loc.lat, loc.lng], { icon: peerIcon }).addTo(map);
        }

        circleMarkers[phone].bindPopup(`
            <b class="font-orbitron" style="color:#BD00FF;">PEER: ${member.name.toUpperCase()}</b><br>
            State: ${isSOS ? '<span style="color:red;font-weight:bold;">SOS DISPATCHED</span>' : 'Secure'}<br>
            Battery: ${loc.battery}% | Speed: ${loc.speed} km/h
        `);
    }

    function renderCircleInvites(invites) {
        const listDiv = document.getElementById("circle-invites-list");
        listDiv.innerHTML = "";

        if (invites.length === 0) {
            listDiv.innerHTML = `<div class="empty-state">No pending inbound security requests.</div>`;
            return;
        }

        invites.forEach(sender => {
            const item = document.createElement("div");
            item.className = "member-item";
            item.innerHTML = `
                <div class="member-info">
                    <span class="member-name">${sender}</span>
                    <span class="member-meta">Inbound connection request link</span>
                </div>
                <div class="action-row">
                    <button class="btn-pill btn-pill-green font-orbitron accept-invite-btn" data-phone="${sender}">ACCEPT</button>
                    <button class="btn-pill btn-pill-red font-orbitron decline-invite-btn" data-phone="${sender}">DECLINE</button>
                </div>
            `;
            listDiv.appendChild(item);
        });

        document.querySelectorAll(".accept-invite-btn").forEach(btn => {
            btn.addEventListener("click", () => respondToInvite(btn.getAttribute("data-phone"), true));
        });
        document.querySelectorAll(".decline-invite-btn").forEach(btn => {
            btn.addEventListener("click", () => respondToInvite(btn.getAttribute("data-phone"), false));
        });
    }

    async function respondToInvite(sender, accept) {
        try {
            const response = await fetch("/api/respond-invite", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ sender_phone: sender, accept })
            });
            if (response.ok) syncCircleState();

        } catch (e) {
            console.error("Invite response synchronization issue");
        }
    }

    // ------------------------------------------
    // FAKE CALL TELEPHONE SIMULATIONS
    // ------------------------------------------
    const triggerCallBtn = document.getElementById("btn-trigger-fake-call");
    const fakeCallerIdSelect = document.getElementById("fake-caller-id");
    const fakeDelaySelect = document.getElementById("fake-delay");

    const activeDeclineBtn = document.getElementById("btn-fake-decline");
    const activeAnswerBtn = document.getElementById("btn-fake-answer");
    const talkingHangupBtn = document.getElementById("btn-fake-hangup");

    const labelIncomingName = document.getElementById("incoming-caller-title");
    const labelOngoingName = document.getElementById("ongoing-caller-title");
    const labelTalkingTimer = document.getElementById("call-talking-timer");

    triggerCallBtn.addEventListener("click", () => {
        const callerName = fakeCallerIdSelect.value;
        const delaySeconds = parseInt(fakeDelaySelect.value);

        labelIncomingName.textContent = callerName.split(' ')[0];
        labelOngoingName.textContent = callerName;

        alert(`Stealth Deterrence booked. Phone simulator will deploy in ${delaySeconds} seconds.`);
        showScreen("dashboard");

        if (fakeCallTimeoutId) clearTimeout(fakeCallTimeoutId);
        fakeCallTimeoutId = setTimeout(deployFakeCallScreen, delaySeconds * 1000);
    });

    function deployFakeCallScreen() {
        showScreen("fakecallActive");
        playPhoneRingtone();
    }

    // WEB AUDIO TELEPHONE CADENCE RING OSCILLATOR
    function playPhoneRingtone() {
        if (!phoneCallAudioCtx) {
            phoneCallAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
        }

        function ringCycle() {
            if (phoneCallAudioCtx.state === 'suspended') phoneCallAudioCtx.resume();
            
            const osc1 = phoneCallAudioCtx.createOscillator();
            const osc2 = phoneCallAudioCtx.createOscillator();
            const gain = phoneCallAudioCtx.createGain();

            osc1.type = 'sine';
            osc1.frequency.setValueAtTime(440, phoneCallAudioCtx.currentTime);
            
            osc2.type = 'sine';
            osc2.frequency.setValueAtTime(480, phoneCallAudioCtx.currentTime);

            gain.gain.setValueAtTime(0, phoneCallAudioCtx.currentTime);
            gain.gain.linearRampToValueAtTime(0.25, phoneCallAudioCtx.currentTime + 0.15);
            gain.gain.setValueAtTime(0.25, phoneCallAudioCtx.currentTime + 1.85);
            gain.gain.linearRampToValueAtTime(0, phoneCallAudioCtx.currentTime + 2.0);

            osc1.connect(gain);
            osc2.connect(gain);
            gain.connect(phoneCallAudioCtx.destination);

            osc1.start();
            osc2.start();

            osc1.stop(phoneCallAudioCtx.currentTime + 2.0);
            osc2.stop(phoneCallAudioCtx.currentTime + 2.0);
        }

        ringCycle();
        phoneRingInterval = setInterval(ringCycle, 4000);
    }

    function stopPhoneRingtone() {
        if (phoneRingInterval) {
            clearInterval(phoneRingInterval);
            phoneRingInterval = null;
        }
    }

    activeDeclineBtn.addEventListener("click", () => {
        stopPhoneRingtone();
        showScreen("dashboard");
    });

    activeAnswerBtn.addEventListener("click", () => {
        stopPhoneRingtone();
        showScreen("fakecallTalking");
        startFakeCallDialogue();
    });

    talkingHangupBtn.addEventListener("click", () => {
        stopFakeCallDialogue();
        showScreen("dashboard");
    });

    function startFakeCallDialogue() {
        fakeCallTalkingTimer = 0;
        labelTalkingTimer.textContent = "00:00";

        fakeCallTalkingInterval = setInterval(() => {
            fakeCallTalkingTimer++;
            const mins = Math.floor(fakeCallTalkingTimer / 60);
            const secs = fakeCallTalkingTimer % 60;
            labelTalkingTimer.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        }, 1000);

        const scripts = [
            "Hello, are you there? I am looking at your safewatch location beacon now.",
            "I'm keeping this lock open. Stay on well-lit streets.",
            "Yes, I see you are on the move. Let's keep talking so they hear a voice line.",
            "I'm updating your travel history logs locally. Let's stay in connection.",
            "Understood. Just walk directly to the safezone locked perimeter."
        ];

        let index = 0;
        function speakNextStatement() {
            if ('speechSynthesis' in window) {
                window.speechSynthesis.cancel();
                const statement = scripts[index];
                const utterance = new SpeechSynthesisUtterance(statement);
                
                utterance.rate = 0.95;
                utterance.pitch = 0.85; // Muffled tone
                
                window.speechSynthesis.speak(utterance);
                
                // Muffled dialtone audio chimes in background to sound more realistic
                beep(350, 0.1); 

                const feedLog = document.getElementById("feed-synthetic-log");
                if (feedLog) feedLog.textContent = `PEER FEED: "${statement}"`;
                index = (index + 1) % scripts.length;
            }
        }

        speakNextStatement();
        fakeCallVoiceInterval = setInterval(speakNextStatement, 7500);
    }

    function stopFakeCallDialogue() {
        if (fakeCallTalkingInterval) {
            clearInterval(fakeCallTalkingInterval);
            fakeCallTalkingInterval = null;
        }
        if (fakeCallVoiceInterval) {
            clearInterval(fakeCallVoiceInterval);
            fakeCallVoiceInterval = null;
        }
        if ('speechSynthesis' in window) {
            window.speechSynthesis.cancel();
        }
    }

    // ------------------------------------------
    // FIREBASE CLOUD MESSAGING (FCM) CONTROLLER
    // ------------------------------------------
    let fcmInitialized = false;
    let localMessaging = null;

    async function initFCM() {
        if (!authToken) return;

        const senderId = document.getElementById("fcm-sender-id").value.trim();
        const projectId = document.getElementById("fcm-project-id").value.trim();
        const vapidKey = document.getElementById("fcm-vapid-key").value.trim();
        const apiKey = document.getElementById("fcm-api-key").value.trim() || "mock-api-key";
        const appId = document.getElementById("fcm-app-id").value.trim() || `1:${senderId}:web:mock`;

        if (!senderId || !projectId) {
            console.warn("FCM config: Sender ID and Project ID are required to initialize FCM.");
            return;
        }

        try {
            // Check if firebase compat SDK is available
            if (typeof firebase === 'undefined') {
                console.error("Firebase SDK is not loaded in the browser.");
                updateFCMStatus("SDK ERROR", "Firebase script tag not loaded", "text-red");
                return;
            }

            // Initialize or retrieve app
            let app;
            if (!firebase.apps.length) {
                app = firebase.initializeApp({
                    apiKey: apiKey,
                    authDomain: `${projectId}.firebaseapp.com`,
                    projectId: projectId,
                    storageBucket: `${projectId}.appspot.com`,
                    messagingSenderId: senderId,
                    appId: appId
                });
            } else {
                app = firebase.app();
            }

            localMessaging = firebase.messaging(app);
            fcmInitialized = true;
            console.log("[FCM] Firebase initialization successful.");

            // Register Service Worker
            if ('serviceWorker' in navigator) {
                const registration = await navigator.serviceWorker.register('/static/firebase-messaging-sw.js');
                localMessaging.useServiceWorker(registration);
                console.log("[FCM] Service worker linked successfully.");
            }

            // Attempt to get token if permission is already granted
            if (Notification.permission === 'granted') {
                await requestAndSyncFCMToken(vapidKey);
            } else {
                updateFCMStatus("UNREGISTERED", "Notification permission not granted. Click Register Device.", "text-blue");
            }

        } catch (error) {
            console.error("[FCM] Initialization error:", error);
            updateFCMStatus("ERROR", error.message, "text-red");
        }
    }

    async function requestAndSyncFCMToken(vapidKey) {
        if (!fcmInitialized || !localMessaging) {
            alert("FCM not initialized. Please configure credentials.");
            return;
        }

        try {
            updateFCMStatus("REGISTERING...", "Requesting permission and token...", "text-blue");
            
            // Request permission explicitly if not granted
            const permission = await Notification.requestPermission();
            if (permission !== 'granted') {
                updateFCMStatus("DENIED", "Notification permission was denied.", "text-red");
                return;
            }

            // Get registration token
            const tokenOptions = vapidKey ? { vapidKey: vapidKey } : {};
            const token = await localMessaging.getToken(tokenOptions);
            
            if (token) {
                // Post token to backend
                const success = await sendTokenToBackend(token);
                if (success) {
                    updateFCMStatus("ACTIVE", token, "text-green");
                } else {
                    updateFCMStatus("SYNC FAILED", "Token fetched but backend update failed.", "text-red");
                }
            } else {
                updateFCMStatus("NO TOKEN", "No registration token available. Check Web Push Certs.", "text-red");
            }
        } catch (error) {
            console.error("[FCM] Token acquisition failed:", error);
            updateFCMStatus("ERROR", error.message, "text-red");
        }
    }

    async function sendTokenToBackend(token) {
        try {
            const res = await fetch("/api/fcm-token", {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({ token: token })
            });
            if (res.ok) {
                console.log("[FCM] Device token registered on backend.");
                return true;
            }
        } catch (e) {
            console.error("[FCM] Failed to send token to backend:", e);
        }
        return false;
    }

    function updateFCMStatus(status, details, colorClass) {
        const badge = document.getElementById("fcm-token-status-badge");
        const display = document.getElementById("fcm-token-display");
        
        if (badge) {
            badge.textContent = status;
            badge.className = `badge-status ${colorClass}`;
        }
        if (display) {
            display.textContent = details;
            display.style.color = status === "ACTIVE" || status === "MOCK_ACTIVE" ? "#00FF41" : "";
        }
    }

    // Set up FCM UI event listeners
    document.getElementById("btn-fcm-register").addEventListener("click", () => {
        const vapidKey = document.getElementById("fcm-vapid-key").value.trim();
        if (!fcmInitialized) {
            initFCM().then(() => {
                if (fcmInitialized) {
                    requestAndSyncFCMToken(vapidKey);
                }
            });
        } else {
            requestAndSyncFCMToken(vapidKey);
        }
    });

    document.getElementById("btn-fcm-mock").addEventListener("click", async () => {
        const mockToken = "mock_fcm_token_" + Math.random().toString(36).substring(2, 15) + "_" + Date.now();
        updateFCMStatus("MOCK_ACTIVE", "Simulating Web Push: " + mockToken, "text-green");
        
        const success = await sendTokenToBackend(mockToken);
        if (success) {
            alert("Mock registration token registered on backend successfully!");
        } else {
            alert("Backend token registration failed.");
        }
    });

    document.getElementById("btn-fcm-test-push").addEventListener("click", async () => {
        if (!authToken) {
            alert("Please log in first.");
            return;
        }

        try {
            const response = await fetch("/api/fcm-test-push", {
                method: "POST",
                headers: authHeaders()
            });
            const data = await response.json();
            if (response.ok) {
                alert("Backend Success: " + data.message);
            } else {
                alert("Backend Error: " + (data.detail || data.message));
            }
        } catch (err) {
            alert("FCM test push request failed.");
        }
    });
});
