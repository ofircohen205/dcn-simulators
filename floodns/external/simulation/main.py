import os
from os import getcwd, remove, makedirs
from subprocess import run
from typer import Typer

from external.schemas.distributed_training import DistributedTraining
from external.schemas.oversubscription import HostOversubscription
from external.schemas.routing import Routing

app = Typer()


def create_shell_file(
    n_tors: int,
    load: float,
    bandwidth: int,
    parallel: DistributedTraining,
    routing: Routing,
    oversubscription: HostOversubscription,
    num_failed_nodes: int = -1,
) -> str:
    runs_dir = os.path.join(
        getcwd(),
        "runs",
        parallel.value,
        "2_level",
        f"{n_tors}_tors",
        f"bw_{bandwidth}",
        f"load_{load}",
    )
    if num_failed_nodes > 0:
        runs_dir = os.path.join(runs_dir, "failures", "node", f"{num_failed_nodes}_nodes")
    else:
        runs_dir = os.path.join(runs_dir, "no_failures")

    runs_dir = os.path.join(runs_dir, oversubscription.value, routing.value)

    log_dir = os.path.join(
        getcwd(),
        "logs",
        "out",
        f"bw_{bandwidth}",
        f"load_{load}",
    )
    if num_failed_nodes > 0:
        log_dir = os.path.join(log_dir, "failures", "node", f"{num_failed_nodes}_nodes")
    else:
        log_dir = os.path.join(log_dir, "no_failures")

    err_dir = log_dir.replace("out", "err")
    makedirs(log_dir, exist_ok=True)
    makedirs(err_dir, exist_ok=True)

    log_file = os.path.join(log_dir, f"%j-{routing.value}-{n_tors}_tors.log")
    err_file = os.path.join(err_dir, f"%j-{routing}-{n_tors}_tors.err")
    job_name = f"{routing.value}-{n_tors}_tors-bw_{bandwidth}-load_{load}"
    if num_failed_nodes > 0:
        job_name = f"{job_name}-{num_failed_nodes}_failed_nodes"
    shell_script = os.path.join(getcwd(), f"{job_name}.sh")
    with open(shell_script, "w") as f:
        f.write("#!/bin/sh\n")
        f.write("#SBATCH --mem=10gb\n")
        f.write("#SBATCH --time=21-0\n")
        f.write(f"#SBATCH --error={err_file}\n")
        f.write(f"#SBATCH --output={log_file}\n")
        f.write(f"#SBATCH --job-name={job_name}\n")
        f.write("\n")
        f.write(f"java -jar floodns-basic-sim.jar {runs_dir}")

    return shell_script


@app.command()
def no_failures(
    n_tors: int,
    load: float,
    bandwidth: int,
    parallel: DistributedTraining,
    routing: Routing,
    oversubscription: HostOversubscription,
):
    shell_script = create_shell_file(
        n_tors=n_tors,
        load=load,
        bandwidth=bandwidth,
        parallel=parallel,
        routing=routing,
        oversubscription=oversubscription,
    )
    print(f"Running {shell_script}")
    run(["chmod", "+x", shell_script])
    run(["sbatch", shell_script])
    remove(shell_script)


@app.command()
def node_failures(
    n_tors: int,
    load: float,
    bandwidth: int,
    num_failed_nodes: int,
    parallel: DistributedTraining,
    routing: Routing,
    oversubscription: HostOversubscription,
):
    shell_script = create_shell_file(
        n_tors=n_tors,
        load=load,
        bandwidth=bandwidth,
        parallel=parallel,
        routing=routing,
        oversubscription=oversubscription,
        num_failed_nodes=num_failed_nodes,
    )
    print(f"Running {shell_script}")
    run(["chmod", "+x", shell_script])
    run(["sbatch", shell_script])
    remove(shell_script)


if __name__ == "__main__":
    app()
