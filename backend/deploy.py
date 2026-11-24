"""Build the arm64 / Python 3.12 Lambda zip and deploy the code (configuration is left as is).

    python3 -m pip install boto3
    AWS_PROFILE=hackathon FUNCTION_NAME=<lambda name> python3 deploy.py
"""
import os
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).parent
BUILD = HERE.parent / "build" / "lambda"
ZIP = HERE.parent / "build" / "verifiedstart-lambda"
SOURCES = ["handler.py", "service.py", "store.py", "stub_verifier.py", "trust.py", "validator.py", "verifier_client.py"]


def build() -> Path:
    shutil.rmtree(BUILD, ignore_errors=True)
    BUILD.mkdir(parents=True)
    subprocess.run([sys.executable, "-m", "pip", "install", "-q", "--target", str(BUILD),
                    "--platform", "manylinux2014_aarch64", "--implementation", "cp", "--python-version", "3.12",
                    "--only-binary=:all:", "-r", str(HERE / "requirements.txt")], check=True)
    for name in SOURCES + [p.name for p in HERE.glob("rules.py")] + [p.name for p in HERE.glob("linker.py")]:
        shutil.copy(HERE / name, BUILD / name)
    return Path(shutil.make_archive(str(ZIP), "zip", BUILD))


def deploy(archive: Path) -> None:
    import boto3

    function = os.environ["FUNCTION_NAME"]
    client = boto3.client("lambda", region_name=os.environ.get("AWS_REGION", "eu-central-1"))
    client.update_function_code(FunctionName=function, ZipFile=archive.read_bytes())
    client.get_waiter("function_updated_v2").wait(FunctionName=function)
    print(f"deployed {archive.name} to {function}")


if __name__ == "__main__":
    deploy(build())
