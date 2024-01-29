import os.path as osp
from os import getcwd, remove, makedirs
from typer import Typer
from subprocess import run

from mlu_lb.schemas.distributed_training import DistributedTraining
from mlu_lb.schemas.routing import Routing

app = Typer()


def create_shell_file(
    n_tors: int,
    load: float,
    bandwidth: int,
    parallel: DistributedTraining,
    link_failure_rate: float,
    routing: str,
) -> str:
    n_cores = n_tors // 2
    n_servers = n_tors * n_cores
    props_file = osp.join(
        getcwd(),
        "properties",
        parallel.value,
        f"{n_tors}_tors",
        f"bw_{bandwidth}",
        f"load_{load}",
        f"link_failure_rate_{link_failure_rate}",
        f"{routing}-fat_tree-{n_tors}_tors-{n_cores}_cores-{n_servers}_servers.properties",
    )
    log_dir = osp.join(
        getcwd(),
        "logs",
        "out",
        f"bw_{bandwidth}",
        f"load_{load}",
        f"link_failure_rate_{link_failure_rate}",
    )
    log_file = osp.join(log_dir, f"%j-{routing}-{n_tors}_tors.log")
    err_dir = osp.join(
        getcwd(),
        "logs",
        "err",
        f"bw_{bandwidth}",
        f"load_{load}",
        f"link_failure_rate_{link_failure_rate}",
    )
    err_file = osp.join(err_dir, f"%j-{routing}-{n_tors}_tors.err")
    makedirs(log_dir, exist_ok=True)
    makedirs(err_dir, exist_ok=True)
    job_name = (
        f"{routing}-{n_tors}_tors-bw_{bandwidth}-load_{load}-link_failure_rate_{link_failure_rate}"
    )
    shell_script = osp.join(getcwd(), f"{job_name}.sh")
    with open(shell_script, "w") as f:
        f.write("#!/bin/sh\n")
        f.write("#SBATCH --mem=10gb\n")
        f.write("#SBATCH --time=21-0\n")
        f.write(f"#SBATCH --error={err_file}\n")
        f.write(f"#SBATCH --output={log_file}\n")
        f.write(f"#SBATCH --job-name={job_name}\n")
        f.write("\n")
        f.write(". /cs/labs/schapiram/ofir.cohen205/ldlb-venv/bin/activate")
        f.write("\n")
        f.write(f"java -jar -ea NetBench.jar {props_file}\n")

    return shell_script


@app.command()
def execute_netbench(
    n_tors: int,
    load: float,
    bandwidth: int,
    link_failure_rate: float,
    parallel: DistributedTraining,
    routing: Routing,
):
    shell_script = create_shell_file(
        n_tors=n_tors,
        load=load,
        bandwidth=bandwidth,
        parallel=parallel,
        link_failure_rate=link_failure_rate,
        routing=routing.value,
    )
    print(f"Running {shell_script}")
    run(["chmod", "+x", shell_script])
    run(["sbatch", shell_script])
    remove(shell_script)


if __name__ == "__main__":
    app()
