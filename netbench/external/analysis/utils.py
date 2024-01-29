import os
from os import getcwd
from typing import Dict

import numpy as np
import pandas as pd


def filter_jobs(dfs: Dict[str, pd.DataFrame]):
    job_ids = set(dfs["ecmp"]["job_id"].unique()).intersection(
        set(dfs["mcvlc"]["job_id"].unique())
    )
    successful_jobs = {}
    failed_jobs = {}
    for job_id in job_ids:
        # Get number of stages
        ecmp_num_stages = int(dfs["ecmp"]["stage"].unique().max()) + 1
        mcvlc_num_stages = int(dfs["mcvlc"]["stage"].unique().max()) + 1
        assert ecmp_num_stages == mcvlc_num_stages
        # Get number of epochs
        ecmp_num_epochs = int(dfs["ecmp"]["epoch"].unique().max())
        mcvlc_num_epochs = int(dfs["mcvlc"]["epoch"].unique().max())
        num_epochs = min(ecmp_num_epochs, mcvlc_num_epochs)

        ecmp_durations = []
        mcvlc_durations = []
        for i in range(1, num_epochs + 1):
            # ECMP average epoch time
            ecmp_slowest_stage = dfs["ecmp"][dfs["ecmp"]["epoch"] == i][
                "duration"
            ].max()
            ecmp_slowest_stage = round(ecmp_slowest_stage / 1e9, 2)
            ecmp_durations.append(ecmp_slowest_stage)

            # Greedy average epoch time
            mcvlc_slowest_stage = dfs["mcvlc"][dfs["mcvlc"]["epoch"] == i][
                "duration"
            ].max()
            mcvlc_slowest_stage = round(mcvlc_slowest_stage / 1e9, 2)
            mcvlc_durations.append(mcvlc_slowest_stage)

        mcvlc_outperforms_ecmp = [
            mcvlc_stage <= ecmp_stage
            for mcvlc_stage, ecmp_stage in zip(mcvlc_durations, ecmp_durations)
        ]
        selected_epochs = [i for i, x in enumerate(mcvlc_outperforms_ecmp) if x]

        # Keep only jobs that have similar average epoch time (within 0.5 seconds)
        successful = len(selected_epochs) > 0

        insert_job(
            jobs=successful_jobs if successful else failed_jobs,
            job_id=job_id,
            ecmp=dfs["ecmp"][dfs["ecmp"]["job_id"] == job_id],
            mcvlc=dfs["mcvlc"][dfs["mcvlc"]["job_id"] == job_id],
            edge_coloring=dfs["edge_coloring"][
                dfs["edge_coloring"]["job_id"] == job_id
            ],
            simulated_annealing=dfs["simulated_annealing"][
                dfs["simulated_annealing"]["job_id"] == job_id
            ],
            ilp_solver=dfs["ilp_solver"][dfs["ilp_solver"]["job_id"] == job_id],
            selected_epochs=selected_epochs,
        )

    return successful_jobs, failed_jobs


def insert_job(
    jobs: dict,
    job_id: int,
    ecmp: pd.Series,
    mcvlc: pd.Series,
    edge_coloring: pd.Series,
    simulated_annealing: pd.Series,
    ilp_solver: pd.Series,
    selected_epochs: list,
):
    jobs[job_id] = {
        "ecmp": {},
        "mcvlc": {},
        "edge_coloring": {},
        "simulated_annealing": {},
        "ilp_solver": {},
    }
    for epoch in selected_epochs:
        jobs[job_id]["ecmp"][epoch] = [
            [int(conn_id) for conn_id in flow_ids.split(";")]
            for flow_ids in ecmp[ecmp["epoch"] == epoch]["flow_ids"].tolist()
        ]

        jobs[job_id]["mcvlc"][epoch] = [
            [int(conn_id) for conn_id in flow_ids.split(";")]
            for flow_ids in mcvlc[mcvlc["epoch"] == epoch]["flow_ids"].tolist()
        ]

        jobs[job_id]["edge_coloring"][epoch] = [
            [int(conn_id) for conn_id in flow_ids.split(";")]
            for flow_ids in edge_coloring[edge_coloring["epoch"] == epoch][
                "flow_ids"
            ].tolist()
        ]

        jobs[job_id]["simulated_annealing"][epoch] = [
            [int(conn_id) for conn_id in flow_ids.split(";")]
            for flow_ids in simulated_annealing[simulated_annealing["epoch"] == epoch][
                "flow_ids"
            ].tolist()
        ]

        jobs[job_id]["ilp_solver"][epoch] = [
            [int(conn_id) for conn_id in flow_ids.split(";")]
            for flow_ids in ilp_solver[ilp_solver["epoch"] == epoch][
                "flow_ids"
            ].tolist()
        ]


def load_job_info(csv_file: str) -> pd.DataFrame | None:
    if not os.path.exists(csv_file):
        return None
    job_info = pd.read_csv(csv_file)
    csv_file = os.path.join(getcwd(), "runs", "headers", "job_info.header")
    job_info.columns = pd.read_csv(csv_file).columns
    job_info.sort_values(by=["job_id", "start_time"], inplace=True)
    job_info.reset_index(drop=True, inplace=True)
    return job_info


def load_flow_info(csv_file: str) -> pd.DataFrame | None:
    if not os.path.exists(csv_file):
        return None
    flow_info = pd.read_csv(csv_file)
    csv_file = os.path.join(getcwd(), "runs", "headers", "flow_info.header")
    flow_info.columns = pd.read_csv(csv_file).columns
    flow_info.sort_values(by=["conn_id", "start_time"], inplace=True)
    return flow_info


def write_job_lines(f, jobs: dict, dfs: dict):
    for job_id, routing_strategies in jobs.items():
        selected_epochs = list(routing_strategies["ecmp"].keys())
        durations = {
            "ecmp": [],
            "mcvlc": [],
            "edge_coloring": [],
            "simulated_annealing": [],
            "ilp_solver": [],
        }
        for epoch in selected_epochs:
            # ECMP average epoch time
            ecmp_slowest_stage = dfs["ecmp"][dfs["ecmp"]["epoch"] == epoch][
                "duration"
            ].max()
            ecmp_slowest_stage = round(ecmp_slowest_stage / 1e9, 2)
            durations["ecmp"].append(ecmp_slowest_stage)

            # Greedy average epoch time
            mcvlc_slowest_stage = dfs["mcvlc"][dfs["mcvlc"]["epoch"] == epoch][
                "duration"
            ].max()
            mcvlc_slowest_stage = round(mcvlc_slowest_stage / 1e9, 2)
            durations["mcvlc"].append(mcvlc_slowest_stage)

            # Edge-Coloring average epoch time
            edge_coloring_slowest_stage = dfs["edge_coloring"][
                dfs["edge_coloring"]["epoch"] == epoch
            ]["duration"].max()
            edge_coloring_slowest_stage = round(edge_coloring_slowest_stage / 1e9, 2)
            durations["edge_coloring"].append(edge_coloring_slowest_stage)

            # Simulated-Annealing average epoch time
            simulated_annealing_slowest_stage = dfs["simulated_annealing"][
                dfs["simulated_annealing"]["epoch"] == epoch
            ]["duration"].max()
            simulated_annealing_slowest_stage = round(
                simulated_annealing_slowest_stage / 1e9, 2
            )
            durations["simulated_annealing"].append(simulated_annealing_slowest_stage)

            # LP-Solver average epoch time
            ilp_solver_slowest_stage = dfs["ilp_solver"][
                dfs["ilp_solver"]["epoch"] == epoch
            ]["duration"].max()
            ilp_solver_slowest_stage = round(ilp_solver_slowest_stage / 1e9, 2)
            durations["ilp_solver"].append(ilp_solver_slowest_stage)

        mcvlc_outperforms_ecmp = [
            mcvlc_stage <= ecmp_stage
            for mcvlc_stage, ecmp_stage in zip(durations["mcvlc"], durations["ecmp"])
        ]
        selected_epochs = [i for i, x in enumerate(mcvlc_outperforms_ecmp) if x]

        avg_durations = {
            "ecmp": round(np.mean([durations["ecmp"][e] for e in selected_epochs]), 2),
            "mcvlc": round(
                np.mean([durations["mcvlc"][e] for e in selected_epochs]), 2
            ),
            "edge_coloring": round(
                np.mean([durations["edge_coloring"][e] for e in selected_epochs]), 2
            ),
            "simulated_annealing": round(
                np.mean([durations["simulated_annealing"][e] for e in selected_epochs]),
                2,
            ),
            "ilp_solver": round(
                np.mean([durations["ilp_solver"][e] for e in selected_epochs]), 2
            ),
        }

        f.write(
            "%-8d %-15s %-16s %-23s %-29s %-20s\n"
            % (
                job_id,
                f"{avg_durations['ecmp']}",
                f"{avg_durations['mcvlc']}",
                f"{avg_durations['edge_coloring']}",
                f"{avg_durations['simulated_annealing']}",
                f"{avg_durations['ilp_solver']}",
            )
        )


def write_flows_metric(f, metric: str, metric_spacing: str, jobs: dict, flows: dict):
    f.write(
        f"Jod ID.  Epoch   ECMP {metric}   Greedy {metric}   Edge-Coloring {metric}   Simulated-Annealing {metric}   ILP Solver {metric}\n"
    )

    metrics = {
        job_id: {epoch: {} for epoch in list(jobs[job_id]["ecmp"].keys())}
        for job_id in jobs
    }
    for routing, routing_groups in flows.items():
        for (job_id, epoch), grouped in routing_groups:
            if epoch not in metrics[job_id]:
                continue
            metrics[job_id][epoch][routing] = get_metric(df=grouped, metric=metric)

    for job_id in jobs:
        for epoch, routing_strategies in metrics[job_id].items():
            ecmp_metric = round(routing_strategies["ecmp"], 2)
            mcvlc_metric = round(routing_strategies["mcvlc"], 2)
            edge_coloring_metric = round(routing_strategies["edge_coloring"], 2)
            simulated_annealing_metric = round(
                routing_strategies["simulated_annealing"], 2
            )
            ilp_solver_metric = round(routing_strategies["ilp_solver"], 2)

            f.write(
                metric_spacing
                % (
                    job_id,
                    epoch,
                    f"{ecmp_metric}",
                    f"{mcvlc_metric}",
                    f"{edge_coloring_metric}",
                    f"{simulated_annealing_metric}",
                    f"{ilp_solver_metric}",
                )
            )


def get_metric(df: pd.DataFrame, metric: str):
    if metric == "Duration":
        return round(df["duration"].max() / 1e9, 2)
    elif metric == "Throughput":
        return round(df["sent"].max() / df["duration"].max(), 2)
    raise ValueError(f"Unknown metric: {metric}")
