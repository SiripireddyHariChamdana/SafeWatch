import json
import urllib.request
import urllib.error
import time

BASE_URL = "http://localhost:8000"

def make_request(path, method="GET", data=None, token=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
        
    req_data = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
    
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode("utf-8")), response.status
    except urllib.error.HTTPError as e:
        error_msg = e.read().decode("utf-8")
        try:
            return json.loads(error_msg), e.code
        except json.JSONDecodeError:
            return {"detail": error_msg}, e.code

def run_simulation():
    print("==========================================================")
    print("           SAFEWATCH COMPREHENSIVE API SIMULATION")
    print("==========================================================")
    
    # 1. Signup user
    print("\n[*] Step 1: Provisioning New Operator Profile...")
    signup_data = {
        "action": "signup",
        "email": "demo_operator@example.com",
        "phone": "+1-555-123-4567",
        "password": "securepassword123",
        "name": "Demo Operator",
        "blood_group": "B+",
        "address": "456 Safety Plaza, Hub C",
        "emergency_phone": "+18597656018"
    }
    res, status = make_request("/api/auth", "POST", signup_data)
    print(f"    Status Code: {status}")
    print(f"    Response: {json.dumps(res, indent=4)}")
    
    # 2. Login user
    print("\n[*] Step 2: Authenticating and Handshaking Gateway...")
    login_data = {
        "action": "login",
        "email": "demo_operator@example.com",
        "password": "securepassword123"
    }
    res, status = make_request("/api/auth", "POST", login_data)
    print(f"    Status Code: {status}")
    token = res.get("access_token")
    print(f"    Obtained JWT Access Token: {token[:30]}...")
    
    # 3. Add emergency contacts
    print("\n[*] Step 3: Registering Emergency Contact Dial...")
    contact_data = {
        "name": "Emergency Contact",
        "phone": "+18597656018",
        "relationship": "Immediate Family"
    }
    res, status = make_request("/api/contacts?action=add_direct", "POST", contact_data, token)
    print(f"    Status Code: {status}")
    print(f"    Updated Contacts: {json.dumps(res, indent=4)}")
    
    # 4. Live Telemetry Tracking
    print("\n[*] Step 4: Simulating Live GPS Location Telemetry Sync...")
    route_points = [
        {"lat": 13.7563, "lng": 100.5018, "speed": 1.5, "battery": 95.0, "signal": 98},
        {"lat": 13.7570, "lng": 100.5025, "speed": 4.8, "battery": 94.0, "signal": 95},
        {"lat": 13.7582, "lng": 100.5036, "speed": 14.2, "battery": 92.0, "signal": 88}
    ]
    
    for i, pt in enumerate(route_points):
        print(f"    [GPS Tick {i+1}] Syncing coordinates: Lat {pt['lat']}, Lng {pt['lng']}, Battery {pt['battery']}%...")
        res, status = make_request("/api/sync", "POST", pt, token)
        print(f"      Response: Safety Score = {res.get('safety_score')}, SOS Active = {res.get('sos_active')}")
        time.sleep(1)
        
    # 5. Trigger SOS Distress Alert
    print("\n[*] Step 5: Manually Triggering SOS Distress Beacon...")
    res, status = make_request("/api/sos/trigger", "POST", None, token)
    print(f"    Status Code: {status}")
    print(f"    SOS Dispatch Actions: {json.dumps(res, indent=4)}")
    
    # 6. Cancel SOS Distress Alert
    print("\n[*] Step 6: Disarming and Cancelling SOS Distress State...")
    res, status = make_request("/api/cancel-sos", "POST", None, token)
    print(f"    Status Code: {status}")
    print(f"    Disarm Response: {json.dumps(res, indent=4)}")
    print("\n==========================================================")
    print("           SAFEWATCH SIMULATION COMPLETED SUCCESSFULLY")
    print("==========================================================")

if __name__ == "__main__":
    run_simulation()
