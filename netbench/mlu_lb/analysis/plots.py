import os.path as osp
from os import getcwd, makedirs
from typer import Typer
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from mlu_lb.schemas.distributed_training import DistributedTraining

app = Typer()


OUTPUT_FOLDER = osp.join(
    getcwd(),
    "experiments",
    "plots",
)

bandwidths = [10, 25, 100, 200, 400]
loads = [0.1, 0.25, 0.5, 0.75, 0.9, 1.0]
link_failure_rates = [0.0, 0.01, 0.05, 0.1]
percentiles = ["Average", "Median", "25th", "75th", "90th", "95th", "99th"]

CDF_FILE = "cdf-bw_{BW}-load_{LOAD}.cdf"


@app.command()
def flows_fct(n_tors: int, parallel: DistributedTraining):
    for link_failure_rate in link_failure_rates:
        plots_helper(n_tors, parallel, link_failure_rate, "FCT")


@app.command()
def flows_throughput(n_tors: int, parallel: DistributedTraining):
    for link_failure_rate in link_failure_rates:
        plots_helper(n_tors, parallel, link_failure_rate, "Throughput")


@app.command()
def jobs_training_iteration_time(n_tors: int, parallel: DistributedTraining):
    for link_failure_rate in link_failure_rates:
        plots_helper(n_tors, parallel, link_failure_rate, "training_iteration_time")


@app.command()
def jobs_throughput(n_tors: int, parallel: DistributedTraining):
    for link_failure_rate in link_failure_rates:
        plots_helper(n_tors, parallel, link_failure_rate, "Throughput")


def plots_helper(n_tors: int, parallel: DistributedTraining, link_failure_rate: float, metric: str):
    plots_folder = osp.join(
        OUTPUT_FOLDER,
        f"{n_tors}_tors",
        parallel.value,
        f"link_failure_rate_{link_failure_rate}",
    )
    for load in loads:
        bandwidths_ecmp_dfs = []
        bandwidths_mcvlc_dfs = []
        bandwidths_edge_coloring_dfs = []
        for bw in bandwidths:
            ecmp_cdf_file = osp.join(
                plots_folder,
                metric.lower(),
                "ecmp",
                CDF_FILE.format(BW=bw, LOAD=load),
            )
            mcvlc_cdf_file = osp.join(
                plots_folder,
                metric.lower(),
                "mcvlc",
                CDF_FILE.format(BW=bw, LOAD=load),
            )
            edge_coloring_cdf_file = osp.join(
                plots_folder,
                metric.lower(),
                "fast_edge_coloring",
                CDF_FILE.format(BW=bw, LOAD=load),
            )
            if (
                not osp.exists(ecmp_cdf_file)
                or not osp.exists(mcvlc_cdf_file)
                or not osp.exists(edge_coloring_cdf_file)
            ):
                continue
            ecmp_cdf = pd.read_csv(ecmp_cdf_file, delimiter="\t", names=[metric, "cdf"])
            mcvlc_cdf = pd.read_csv(mcvlc_cdf_file, delimiter="\t", names=[metric, "cdf"])
            edge_coloring_cdf = pd.read_csv(
                edge_coloring_cdf_file, delimiter="\t", names=[metric, "cdf"]
            )

            data = [ecmp_cdf[metric], mcvlc_cdf[metric], edge_coloring_cdf[metric]]

            load_folder = osp.join(plots_folder, f"load_{load}")
            makedirs(load_folder, exist_ok=True)
            plot_boxplot(data, load, bw, link_failure_rate, load_folder, metric)
            plt.clf()
            plot_cdf(
                ecmp_cdf,
                mcvlc_cdf,
                edge_coloring_cdf,
                load,
                bw,
                link_failure_rate,
                load_folder,
                metric,
            )
            plt.clf()

            bandwidths_ecmp_dfs.append(ecmp_cdf)
            bandwidths_mcvlc_dfs.append(mcvlc_cdf)
            bandwidths_edge_coloring_dfs.append(edge_coloring_cdf)

        size_ecmp_dfs = len(bandwidths_ecmp_dfs)
        size_mcvlc_dfs = len(bandwidths_mcvlc_dfs)
        size_edge_coloring_dfs = len(bandwidths_edge_coloring_dfs)
        if size_ecmp_dfs == size_mcvlc_dfs == size_edge_coloring_dfs == len(bandwidths):
            plot_across_bandwidths(
                bandwidths_ecmp_dfs,
                bandwidths_mcvlc_dfs,
                bandwidths_edge_coloring_dfs,
                load,
                plots_folder,
                link_failure_rate,
                metric,
            )
            plt.clf()


def plot_boxplot(
    data: list, load: float, bw: int, link_failure_rate: float, load_folder: str, metric: str
):
    header = get_header(metric)

    ecmp_p90 = np.percentile(data[0], 90)
    ecmp_p99 = np.percentile(data[0], 99)
    mcvlc_p90 = np.percentile(data[1], 90)
    mcvlc_p99 = np.percentile(data[1], 99)
    edge_coloring_p90 = np.percentile(data[2], 90)
    edge_coloring_p99 = np.percentile(data[2], 99)

    fig = plt.figure(figsize=(10, 7))
    ax = fig.add_subplot(111)
    ax.boxplot(
        list(map(lambda x: x.values, data)),
        labels=["ECMP", "MCVLC", "Fast-Edge-Coloring"],
        showmeans=True,
        meanline=True,
        medianprops={"linewidth": 1, "color": "red"},
        meanprops={"linewidth": 1, "color": "blue"},
    )

    ax.set_title(
        (
            "ECMP vs MCVLC vs Fast-Edge-Coloring "
            f"\n{header} \n(load={load}, bw={bw}, link failure rate={link_failure_rate})"
        )
    )
    ax.set_xlabel("Routing")
    ax.set_ylabel(f"{header}")

    plt.plot([0.95, 1.05], [ecmp_p90, ecmp_p90], color="green", linestyle="-.", markersize=10)
    plt.plot([1.95, 2.05], [mcvlc_p90, mcvlc_p90], color="green", linestyle="-.", markersize=10)
    plt.plot(
        [2.95, 3.05],
        [edge_coloring_p90, edge_coloring_p90],
        color="green",
        linestyle="-.",
        markersize=10,
    )

    plt.plot([0.95, 1.05], [ecmp_p99, ecmp_p99], color="black", linestyle=":", markersize=10)
    plt.plot([1.95, 2.05], [mcvlc_p99, mcvlc_p99], color="black", linestyle=":", markersize=10)
    plt.plot(
        [2.95, 3.05],
        [edge_coloring_p99, edge_coloring_p99],
        color="black",
        linestyle=":",
        markersize=10,
    )

    median_line = plt.Line2D([0], [0], linestyle="--", color="red", label="Median")
    mean_line = plt.Line2D([0], [0], linestyle="--", color="blue", label="Mean")
    p90_line = plt.Line2D([0], [0], linestyle="-.", color="green", label="90th")
    p99_line = plt.Line2D([0], [0], linestyle=":", color="black", label="99th")
    ax.legend(handles=[median_line, mean_line, p90_line, p99_line])

    makedirs(osp.join(load_folder, "boxplot"), exist_ok=True)
    plt.savefig(osp.join(load_folder, "boxplot", f"{metric.lower()}-bw_{bw}.png"))


def plot_cdf(
    ecmp_cdf: pd.DataFrame,
    mcvlc_cdf: pd.DataFrame,
    edge_coloring_cdf: pd.DataFrame,
    load: float,
    bw: int,
    link_failure_rate: float,
    load_folder: str,
    metric: str,
):
    header = get_header(metric)

    fig = plt.figure(figsize=(10, 7))
    ax = fig.add_subplot(111)
    ax.set_title(
        (
            "ECMP vs MCVLC vs Fast-Edge-Coloring "
            f"\n{header} \n(load={load}, bw={bw}, link failure rate={link_failure_rate})"
        )
    )
    ax.set_xlabel(f"{header}")
    ax.set_ylabel("CDF")
    ax.plot(ecmp_cdf[metric], ecmp_cdf["cdf"], label="ECMP")
    ax.plot(mcvlc_cdf[metric], mcvlc_cdf["cdf"], label="MCVLC")
    ax.plot(edge_coloring_cdf[metric], edge_coloring_cdf["cdf"], label="Fast-Edge-Coloring")
    ax.legend()
    makedirs(osp.join(load_folder, "cdf"), exist_ok=True)
    plt.savefig(osp.join(load_folder, "cdf", f"{metric.lower()}-bw_{bw}.png"))


def plot_across_bandwidths(
    ecmp_dfs: list,
    mcvlc_dfs: list,
    edge_coloring_dfs: list,
    load: float,
    plots_folder: str,
    link_failure_rate: float,
    metric: str,
):
    for percentile in percentiles:
        plot_across_bandwidths_helper(
            ecmp_dfs,
            mcvlc_dfs,
            edge_coloring_dfs,
            load,
            plots_folder,
            link_failure_rate,
            percentile,
            metric,
        )
        plt.clf()


def plot_across_bandwidths_helper(
    ecmp_dfs: list,
    mcvlc_dfs: list,
    edge_coloring_dfs: list,
    load: float,
    plots_folder: str,
    link_failure_rate: float,
    percentile: str,
    metric: str,
):
    header = get_header(metric)

    fig = plt.figure(figsize=(10, 7))
    ax = fig.add_subplot(111)
    ax.set_title(
        (
            "ECMP vs MCVLC vs Fast-Edge-Coloring"
            f"{percentile}\n"
            f"{header}\n"
            f"(load={load}, link failure rate={link_failure_rate})"
        )
    )
    ax.set_xlabel("Bandwidth (Gbps)")
    ax.set_ylabel(f"{header}")

    ecmp_bandwidths_y = [get_metric_percentile(ecmp_df, percentile, metric) for ecmp_df in ecmp_dfs]
    mcvlc_bandwidths_y = [
        get_metric_percentile(mcvlc_df, percentile, metric) for mcvlc_df in mcvlc_dfs
    ]
    edge_coloring_bandwidths_y = [
        get_metric_percentile(edge_coloring_df, percentile, metric)
        for edge_coloring_df in edge_coloring_dfs
    ]
    ax.plot(bandwidths, ecmp_bandwidths_y, "--o", label="ECMP")
    ax.plot(bandwidths, mcvlc_bandwidths_y, "--x", label="mcvlc")
    ax.plot(bandwidths, edge_coloring_bandwidths_y, "--^", label="Fast-Edge-Coloring")
    ax.legend()
    makedirs(osp.join(plots_folder, "across_bandwidths"), exist_ok=True)
    plt.savefig(
        osp.join(
            plots_folder, "across_bandwidths", f"{percentile.lower()}-{metric}-load_{load}.png"
        )
    )


def get_metric_percentile(df: pd.DataFrame, percentile: str, metric: str):
    return {
        "Average": round(df[metric].mean(), 2),
        "Median": round(df[metric].median(), 2),
        "25th": round(df[metric].quantile(0.25), 2),
        "75th": round(df[metric].quantile(0.75), 2),
        "90th": round(df[metric].quantile(0.9), 2),
        "95th": round(df[metric].quantile(0.95), 2),
        "99th": round(df[metric].quantile(0.99), 2),
    }[percentile]


def get_header(metric: str):
    header = metric
    if metric == "training_iteration_time":
        header = "Training Iteration Time (sec)"
    elif metric == "FCT":
        header = "Flow Completion Time (sec)"
    elif metric == "Throughput":
        header = "Throughput (Gbps)"
    return header


if __name__ == "__main__":
    app()
