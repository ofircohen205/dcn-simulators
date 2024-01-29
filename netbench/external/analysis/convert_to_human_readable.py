import os
from os import getcwd

import pandas as pd
import typer

from external.schemas.routing import Routing

app = typer.Typer()


@app.command()
def concurrent_jobs(routing: Routing, num_concurrent_jobs: int, num_cores: int, ring_size: int):
    folder = os.path.join(
        getcwd(),
        "runs",
        f"concurrent_jobs_{num_concurrent_jobs}",
        f"{num_cores}_core_failures",
        f"ring_size_{ring_size}",
        routing.value,
        "logs",
    )
    if not os.path.exists(folder):
        print(f"logs does not exist for {folder}.")
        return
    convert_to_human_readable_helper(logs_dir=folder)
    print("-" * 100)

    num_failed_cores = [0, 1, 4, 8]
    ring_sizes = [2, 4, 8]
    for num_cores in num_failed_cores:
        for ring_size in ring_sizes:
            base_folder = os.path.join(
                getcwd(),
                "runs",
                f"concurrent_jobs_{num_concurrent_jobs}",
                f"{num_cores}_core_failures",
                f"ring_size_{ring_size}",
                routing.value,
            )
            if not os.path.exists(os.path.join(base_folder, "logs")):
                print(f"logs_floodns does not exist for {os.path.join(base_folder, 'logs')}.")
                continue
            convert_to_human_readable_helper(logs_dir=os.path.join(base_folder, "logs"))
            print("-" * 100)


@app.command()
def basic_sim(experiment_dir: str):
    convert_to_human_readable_helper(logs_dir=os.path.join(experiment_dir, "logs"))


def convert_to_human_readable_helper(logs_dir: str):
    convert_job_info_to_human_readable(logs_floodns_dir=logs_dir)
    convert_flow_info_to_human_readable(logs_floodns_dir=logs_dir)


def convert_job_info_to_human_readable(logs_floodns_dir: str):
    csv_file = os.path.join(logs_floodns_dir, "job_info.csv")
    if not os.path.exists(csv_file):
        print(f"{csv_file} does not exist.")
        return
    job_info = pd.read_csv(csv_file, header=None)
    if job_info.empty:
        print("No jobs found.")
        return
    csv_file = os.path.join(getcwd(), "runs", "headers", "job_info.header")
    job_info.columns = pd.read_csv(csv_file).columns
    job_info.sort_values(by=["start_time", "job_id"], inplace=True)
    job_info.drop(columns=["Unnamed: 7"], inplace=True)
    with open(logs_floodns_dir + "/job_info.txt", "w+") as f:
        # Header
        f.write(
            "Job ID    Epoch    Stage Index     Start time         End time           Duration         "
            "Finished    Total Flows      Flow Size         Connections' IDs\n"
        )

        for i, row in job_info.iterrows():
            f.write(
                "%-9d %-8d %-15d %-18s %-18s %-16s %-11s %-16d %-17s %s\n"
                % (
                    row["job_id"],  # Job ID
                    row["epoch"],  # Epoch
                    row["stage"],  # Stage Index
                    f"{(float(row['start_time']) / 1_000_000_000.):.2f} s",  # Start time (seconds)
                    f"{(float(row['end_time']) / 1_000_000_000.0):.2f} s",  # End time (seconds)
                    f"{float(row['duration'] / 1_000_000_000.0):.2f} s",
                    row["finished"],  # Finished? ["Y", "N"]
                    row["total_flows"],  # Total Flows
                    "%.2f Gbit" % (float(row["flow_size"]) / 1_000_000_000.0),  # Flow Size
                    row["flow_ids"],  # Connections' IDs
                )
            )

        print("Human readable file: " + logs_floodns_dir + "/job_info.txt")


def convert_flow_info_to_human_readable(logs_floodns_dir: str):
    csv_file = os.path.join(logs_floodns_dir, "flow_info.csv")
    if not os.path.exists(csv_file):
        print(f"{csv_file} does not exist.")
        return
    flow_info = pd.read_csv(csv_file, header=None)
    if flow_info.empty:
        print("No flows found.")
        return
    csv_file = os.path.join(getcwd(), "runs", "headers", "flow_info.header")
    flow_info.columns = pd.read_csv(csv_file).columns
    flow_info.sort_values(by=["flow_id", "start_time"], inplace=True)
    with open(logs_floodns_dir + "/flow_info.txt", "w+") as f:
        # Header
        f.write(
            "Flow ID  Job ID  Source  Target   Path            Start time      End time      Duration         "
            "Amount sent   Flow Size     Is Completed\n"
        )

        for i, row in flow_info.iterrows():
            f.write(
                "%-10d %-10d %-8d %-8d %-21s %-18s %-18s %-16s %-15s %-17s %s\n"
                % (
                    row["flow_id"],  # Flow ID
                    row["job_id"],  # Job ID
                    row["source_node_id"],  # Source
                    row["dest_node_id"],  # Target
                    print_path(row),  # Path
                    "%.2f s" % (float(row["flow_start_time"]) / 1_000_000_000.0),  # Start time
                    "%.2f s" % (float(row["flow_end_time"]) / 1_000_000_000.0),  # End time
                    "%.2f s" % (float(row["flow_total_time"]) / 1_000_000_000.0),  # Duration
                    "%.2f Gbit"
                    % (float(row["total_bytes_received"]) / 1_000_000_000.0),  # Amount sent
                    "%.2f Gbit" % (float(row["flow_size_bytes"]) / 1_000_000_000.0),  # Amount sent
                    row["flow_completed"],  # Is Completed? ["Y", "N"]
                )
            )

        print("Human readable file: " + logs_floodns_dir + "/flow_info.txt")


def print_path(row: pd.Series) -> str:
    return ",".join(
        [
            row["source_id"],
            row["source_tor_id"],
            row["core_id"],
            row["target_tor_id"],
            row["target_id"],
        ]
    )


if __name__ == "__main__":
    app()
