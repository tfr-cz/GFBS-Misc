import select
import socket
import threading
from urllib.parse import urlparse

LISTEN = ("127.0.0.1", 8899)
FORGE_HOSTS = {"maven.minecraftforge.net", "files.minecraftforge.net"}
FORGE_IP = "104.26.3.187"


def pipe(left, right):
    sockets = [left, right]
    try:
        while True:
            readable, _, exceptional = select.select(sockets, [], sockets, 300)
            if exceptional or not readable:
                break
            for source in readable:
                target = right if source is left else left
                data = source.recv(65536)
                if not data:
                    return
                target.sendall(data)
    finally:
        for sock in sockets:
            try:
                sock.close()
            except OSError:
                pass


def connect(host, port):
    address = FORGE_IP if host in FORGE_HOSTS else host
    return socket.create_connection((address, port), timeout=90)


def handle(client):
    try:
        header = b""
        while b"\r\n\r\n" not in header:
            chunk = client.recv(8192)
            if not chunk:
                return
            header += chunk
        head, _, rest = header.partition(b"\r\n\r\n")
        line = head.split(b"\r\n", 1)[0].decode("ascii", "replace")
        parts = line.split()
        if len(parts) != 3:
            return
        method, target, _ = parts
        if method.upper() == "CONNECT":
            host, port = target.rsplit(":", 1)
            remote = connect(host, int(port))
            client.sendall(b"HTTP/1.1 200 Connection Established\r\n\r\n")
            if rest:
                remote.sendall(rest)
            print("CONNECT", host, flush=True)
        else:
            parsed = urlparse(target)
            if not parsed.hostname:
                return
            port = parsed.port or (443 if parsed.scheme == "https" else 80)
            remote = connect(parsed.hostname, port)
            path = parsed.path or "/"
            if parsed.query:
                path += "?" + parsed.query
            request = f"{method} {path} HTTP/1.1\r\n".encode() + head.split(b"\r\n", 1)[1] + b"\r\n\r\n" + rest
            remote.sendall(request)
            print("HTTP", parsed.hostname, flush=True)
        pipe(client, remote)
    except Exception as exception:
        print("ERR", exception, flush=True)
    finally:
        try:
            client.close()
        except OSError:
            pass


def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(LISTEN)
    server.listen(64)
    print("listening", LISTEN, flush=True)
    while True:
        client, _ = server.accept()
        threading.Thread(target=handle, args=(client,), daemon=True).start()


if __name__ == "__main__":
    main()
