import socket
import py_eureka_client.eureka_client as eureka_client

async def register_with_eureka():

    hostname = socket.gethostname()
    ip_addr = socket.gethostbyname(hostname)

    await eureka_client.init_async(
        eureka_server="http://registry:8761/eureka",
        app_name="ML-SERVICE",
        instance_port=9090,
        instance_ip=ip_addr,
    )