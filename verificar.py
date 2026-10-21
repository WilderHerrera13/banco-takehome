#!/usr/bin/env python3
"""Verificacion funcional del take-home contra el stack Docker.

    python3 verificar.py            # usa el stack ya levantado
    python3 verificar.py --reset    # reinicia el stack con datos limpios y verifica

Comprueba los requisitos F1-F4 extremo a extremo y la comunicacion asincrona.
"""
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request

MS1 = "http://localhost:8081"
MS2 = "http://localhost:8082"

passed = 0
failed = 0


def call(method, url, body=None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"} if data else {}
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            raw = r.read().decode()
            try:
                return r.status, json.loads(raw)
            except json.JSONDecodeError:
                return r.status, raw
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, raw
    except Exception as e:
        return 0, str(e)


def check(name, expected, actual):
    global passed, failed
    if expected == actual:
        print(f"PASS  {name:<58} {actual}")
        passed += 1
    else:
        print(f"FAIL  {name:<58} esperado[{expected}] obtenido[{actual}]")
        failed += 1


def contains(name, fragment, body):
    global passed, failed
    if fragment in str(body):
        print(f"PASS  {name:<58} ok")
        passed += 1
    else:
        print(f"FAIL  {name:<58} no contiene [{fragment}]")
        failed += 1


def status(name, expected, method, url, body=None):
    global passed, failed
    code, payload = call(method, url, body)
    if code == expected:
        print(f"PASS  {name:<58} HTTP {code}")
        passed += 1
    else:
        print(f"FAIL  {name:<58} HTTP {code} esperado {expected} :: {str(payload)[:140]}")
        failed += 1
    return payload


def wait_healthy(timeout=240):
    deadline = time.time() + timeout
    while time.time() < deadline:
        code1, _ = call("GET", f"{MS1}/clientes")
        code2, _ = call("GET", f"{MS2}/cuentas")
        if code1 == 200 and code2 == 200:
            return True
        time.sleep(3)
    return False


def sql(db, query):
    out = subprocess.run(
        ["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "postgres", "-d", db, "-t", "-c", query],
        capture_output=True, text=True)
    return out.stdout.strip()


def main():
    if "--reset" in sys.argv:
        print("▸ reinicio el stack con datos limpios (docker compose down -v && up -d)")
        subprocess.run(["docker", "compose", "down", "-v"], capture_output=True)
        subprocess.run(["docker", "compose", "up", "-d"], capture_output=True)
    if not wait_healthy():
        print("TIMEOUT: los servicios no respondieron")
        return 1

    print("\n════════ F1 · Cliente: CRUD completo ════════")
    created = status("POST /clientes (crear)", 201, "POST", f"{MS1}/clientes", {
        "name": "Ana Torres", "gender": "FEMALE", "age": 29, "identification": "1712345701",
        "address": "Calle 100 # 10-20", "phone": "3112223344", "password": "clave123", "active": True})
    client_id = created.get("clientId", "")
    check("  el id del cliente creado es un UUID", True, len(client_id) == 36)
    check("  el nombre vuelve correctamente", "Ana Torres", created.get("name"))
    check("  el genero se traduce al enum", "FEMALE", created.get("gender"))
    check("  el estado vuelve correctamente", True, created.get("active"))
    listed = call("GET", f"{MS1}/clientes")[1]
    check("  GET /clientes devuelve los 3 sembrados + 1 creado", 4, len(listed))
    status("GET /clientes/{id}", 200, "GET", f"{MS1}/clientes/{client_id}")
    body = status("GET /clientes/{id} inexistente", 404, "GET", f"{MS1}/clientes/no-existe")
    contains("  el 404 trae el mensaje de error", "Client not found", body)
    status("PUT /clientes/{id} (actualizar)", 200, "PUT", f"{MS1}/clientes/{client_id}", {
        "name": "Ana Torres Actualizada", "gender": "FEMALE", "age": 30, "identification": "1712345701",
        "address": "Calle 100 # 10-20", "phone": "3112223344", "password": "clave123", "active": True})
    check("  el PUT guardo el nombre nuevo", "Ana Torres Actualizada",
          call("GET", f"{MS1}/clientes/{client_id}")[1].get("name"))
    status("POST con identificacion duplicada", 409, "POST", f"{MS1}/clientes", {
        "name": "Dup", "gender": "MALE", "age": 30, "identification": "1712345701",
        "address": "x", "phone": "y", "password": "z", "active": True})
    status("POST con datos invalidos", 400, "POST", f"{MS1}/clientes", {
        "name": "", "gender": "MALE", "age": 30, "identification": "",
        "address": "x", "phone": "y", "password": "z", "active": True})
    status("DELETE /clientes/{id}", 204, "DELETE", f"{MS1}/clientes/{client_id}")
    status("GET /clientes/{id} tras el DELETE", 404, "GET", f"{MS1}/clientes/{client_id}")

    print("\n════════ F1 · Cuenta y Movimiento: CRU ════════")
    status("POST /cuentas", 201, "POST", f"{MS2}/cuentas", {
        "accountNumber": "778899", "accountType": "SAVINGS", "initialBalance": 1000.00,
        "active": True, "clientId": "cli-jose-lema"})
    check("  la cuenta creada tiene saldo inicial 1000", 1000.0,
          call("GET", f"{MS2}/cuentas/778899")[1].get("initialBalance"))
    accounts = call("GET", f"{MS2}/cuentas?clientId=cli-jose-lema")[1]
    check("  el cliente Jose Lema tiene 3 cuentas (2 sembradas + 1 creada)", 3, len(accounts))
    status("GET /cuentas/{id}", 200, "GET", f"{MS2}/cuentas/778899")
    status("GET /cuentas/{id} inexistente", 404, "GET", f"{MS2}/cuentas/no-existe")
    status("PUT /cuentas/{id}", 200, "PUT", f"{MS2}/cuentas/778899", {
        "accountNumber": "778899", "accountType": "CURRENT", "initialBalance": 1000.00,
        "active": True, "clientId": "cli-jose-lema"})
    check("  el PUT cambio el tipo de cuenta", "CURRENT",
          call("GET", f"{MS2}/cuentas/778899")[1].get("accountType"))

    print("\n════════ F2 · Registro de movimientos ════════")
    saldo_antes = call("GET", f"{MS2}/cuentas/225487")[1]["currentBalance"]
    status("POST /movimientos (deposito)", 201, "POST", f"{MS2}/movimientos/225487",
           {"date": "2022-02-25", "movementType": "DEPOSIT", "amount": 600.00})
    saldo_despues = call("GET", f"{MS2}/cuentas/225487")[1]["currentBalance"]
    check("  el deposito sumo 600 al saldo", saldo_antes + 600, saldo_despues)
    movimiento = call("GET", f"{MS2}/movimientos?accountNumber=225487")[1][0]
    check("  el movimiento guarda el saldo resultante", saldo_despues, movimiento["balance"])
    check("  el movimiento guarda el valor con signo", 600.0, movimiento["amount"])
    status("POST /movimientos (retiro valido)", 201, "POST", f"{MS2}/movimientos/225487",
           {"date": "2022-02-26", "movementType": "WITHDRAWAL", "amount": 200.00})
    saldo_final = call("GET", f"{MS2}/cuentas/225487")[1]["currentBalance"]
    check("  tras el retiro el saldo baja 200", saldo_despues - 200, saldo_final)
    status("GET /movimientos/{id}", 200, "GET", f"{MS2}/movimientos/1")
    status("PUT /movimientos/{id}", 200, "PUT", f"{MS2}/movimientos/1",
           {"date": "2022-02-11", "movementType": "DEPOSIT", "amount": 600.00})
    status("POST /movimientos en cuenta inexistente", 404, "POST", f"{MS2}/movimientos/no-existe",
           {"date": "2022-02-27", "movementType": "DEPOSIT", "amount": 10.00})

    print("\n════════ F3 · Saldo no disponible ════════")
    body = status("POST /movimientos con saldo insuficiente", 422, "POST", f"{MS2}/movimientos/225487",
                  {"date": "2022-02-28", "movementType": "WITHDRAWAL", "amount": 999999.00})
    contains("  el mensaje es exactamente 'Saldo no disponible'", "Saldo no disponible", body)
    contains("  el detalle trae el saldo disponible", "availableBalance", body)
    contains("  el detalle trae el valor solicitado", "requestedAmount", body)
    check("  el saldo no cambio tras el rechazo", saldo_final,
          call("GET", f"{MS2}/cuentas/225487")[1]["currentBalance"])

    print("\n════════ F4 · Reporte de estado de cuenta ════════")
    reporte = call("GET", f"{MS2}/reportes?fecha=2022-02-01..2022-02-28&cliente=cli-marianela-montalvo")[1]
    check("  el reporte trae el nombre del cliente", "Marianela Montalvo", reporte.get("client"))
    check("  el reporte trae las 2 cuentas del cliente", 2, len(reporte.get("accounts", [])))
    cuenta = [a for a in reporte["accounts"] if a["accountNumber"] == "225487"][0]
    check("  la cuenta 225487 aparece con su saldo actual", saldo_final, cuenta["currentBalance"])
    check("  la cuenta 225487 conserva el saldo inicial", 100.0, cuenta["initialBalance"])
    check("  el reporte trae los movimientos del periodo", 4, len(reporte.get("movements", [])))
    check("  el reporte respeta el rango pedido", "2022-02-01", reporte.get("from"))
    status("GET /reportes con cliente desconocido", 404, "GET",
           f"{MS2}/reportes?fecha=2022-02-01..2022-02-28&cliente=cli-999")
    status("GET /reportes con rango invalido", 400, "GET",
           f"{MS2}/reportes?fecha=basura&cliente=cli-marianela-montalvo")

    print("\n════════ Comunicacion asincrona entre microservicios ════════")
    check("  ms2 proyecta los clientes publicados por ms1", "1",
          sql("banco_ms2", "select count(*) from client_projection where client_id='cli-jose-lema'"))
    check("  ms1 proyecta los movimientos publicados por ms2", "1",
          sql("banco_ms1", "select count(*) from movement_projection where movement_id=1"))

    print("\n════════════════════════════════════════════")
    print(f"RESULTADO: {passed} correctos, {failed} fallidos")
    print("TODO OK" if failed == 0 else "HAY FALLOS")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
