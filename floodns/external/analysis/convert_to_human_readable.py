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
        "logs_floodns",
    )
    if not os.path.exists(folder):
        print(f"logs_floodns does not exist for {folder}.")
        return
    convert_to_human_readable_helper(logs_floodns_dir=folder)
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
            if not os.path.exists(os.path.join(base_folder, "logs_floodns")):
                print(
                    f"logs_floodns does not exist for {os.path.join(base_folder, 'logs_floodns')}."
                )
                continue
            convert_to_human_readable_helper(
                logs_floodns_dir=os.path.join(base_folder, "logs_floodns")
            )
            print("-" * 100)


@app.command()
def basic_sim(experiment_dir: str):
    convert_to_human_readable_helper(logs_floodns_dir=os.path.join(experiment_dir, "logs_floodns"))


def convert_to_human_readable_helper(logs_floodns_dir: str):
    convert_job_info_to_human_readable(logs_floodns_dir=logs_floodns_dir)
    convert_connection_info_to_human_readable(logs_floodns_dir=logs_floodns_dir)
    convert_flow_info_to_human_readable(logs_floodns_dir=logs_floodns_dir)


def convert_connection_info_to_human_readable(logs_floodns_dir: str):
    csv_file = os.path.join(logs_floodns_dir, "connection_info.csv")
    if not os.path.exists(csv_file):
        print(f"{csv_file} does not exist.")
        return
    connection_info = pd.read_csv(csv_file, header=None)
    if connection_info.empty:
        print("No connections found.")
        return
    csv_file = os.path.join(getcwd(), "runs", "headers", "connection_info.header")
    connection_info.columns = pd.read_csv(csv_file).columns
    connection_info.sort_values(by=["start_time", "conn_id"], inplace=True)
    with open(logs_floodns_dir + "/connection_info.txt", "w+") as f:
        # Header
        f.write(
            "Job ID  Epoch  Stage  Conn. ID   Source   Target   Size           Sent           Flows' IDs     Start time         "
            "End time           Duration         Progress     Avg. rate        Finished?     Metadata\n"
        )

        for i, row in connection_info.iterrows():
            # check if row["metadata"] is NaN
            if row["metadata"] != row["metadata"]:
                row["metadata"] = ""
            f.write(
                "%-7d %-6d %-6d %-10d %-8d %-8d %-14s %-14s %-15s %-18s %-18s %-16s %-14s %-16s %-14s %s\n"
                % (
                    row["job_id"],  # Job ID
                    row["epoch"],  # Epoch
                    row["stage"],  # Stage
                    row["conn_id"],  # Connection ID
                    row["src_id"],  # Source
                    row["dst_id"],  # Target
                    "%.2f Gbit" % (float(row["size"]) / 1_000_000_000.0),  # Size
                    "%.2f Gbit" % (float(row["sent"]) / 1_000_000_000.0),  # Sent
                    row["flow_ids"],  # Flows' IDs
                    "%.2f s" % (float(row["start_time"]) / 1_000_000_000.0),  # Start time
                    "%.2f s" % (float(row["end_time"]) / 1_000_000_000.0),  # End time
                    "%.2f s" % (float(row["duration"]) / 1_000_000_000.0),  # Duration
                    "%.2f %%" % (float(row["sent"]) / float(row["size"]) * 100.0),  # Progress
                    "%.2f Gbit/s" % (float(row["avg_rate"])),  # Avg. rate
                    row["finished"],  # Finished? ["Y", "N"]
                    row["metadata"].strip(),  # Metadata
                )
            )

        print("Human readable file: " + logs_floodns_dir + "/connection_info.txt")


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
                    row["conn_ids"],  # Connections' IDs
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
            "Flow ID    Source   Target   Path                  Start time         End time           Duration         "
            "Amount sent     Avg. bandwidth   Metadata\n"
        )

        for i, row in flow_info.iterrows():
            # check if row["metadata"] is NaN
            if row["metadata"] != row["metadata"]:
                row["metadata"] = ""
            f.write(
                "%-10d %-8d %-8d %-21s %-18s %-18s %-16s %-15s %-17s %s\n"
                % (
                    row["flow_id"],  # Flow ID
                    row["source_node_id"],  # Source
                    row["dest_node_id"],  # Target
                    print_path(row["path"]),  # Path
                    "%.2f s" % (float(row["start_time"]) / 1_000_000_000.0),  # Start time
                    "%.2f s" % (float(row["end_time"]) / 1_000_000_000.0),  # End time
                    "%.2f s" % (float(row["duration"]) / 1_000_000_000.0),  # Duration
                    "%.2f Gbit" % (float(row["amount_sent"]) / 1_000_000_000.0),  # Amount sent
                    "%.2f Gbit/s" % (float(row["average_bandwidth"])),  # Average bandwidth
                    row["metadata"].strip(),  # Metadata
                )
            )

        print("Human readable file: " + logs_floodns_dir + "/flow_info.txt")


def print_path(path: str) -> str:
    """
    :param path: e.g. "node1_id-[link1_id]->node2_id-[link2_id]->...-last_link_id->[last_node_id]"
    :return: e.g. "node1_id, node2_id, ..., last_node_id"
    """
    path = path.split("->")
    path = [node.split("-")[0] for node in path]
    return ",".join(path)


if __name__ == "__main__":
    app()
