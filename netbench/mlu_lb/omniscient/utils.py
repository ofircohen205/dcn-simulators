import os.path as osp
from os import getcwd, listdir
import pandas as pd

from mlu_lb.schemas.distributed_training import DistributedTraining


def load_virtual_rings(
    level: int, tors: int, bw: int, load: float, parallel: DistributedTraining, ring_ids: list
) -> dict:
    cores = tors // 2
    servers = cores * tors

    virtual_rings_dir = osp.join(
        getcwd(),
        "traffic_pairs",
        f"{level}_level",
        f"{tors}_tors",
        f"{cores}_cores",
        f"{servers}_servers",
        parallel.value,
        "42",
        f"load_{load}",
    )

    virtual_rings = {}
    for dirname in listdir(virtual_rings_dir):
        if not dirname.startswith("job_"):
            continue
        ring_id = int(dirname.split("-")[0].split("_")[-1])
        if ring_id not in ring_ids:
            continue
        filename = osp.join(virtual_rings_dir, dirname, "data_parallelism.txt")
        df = pd.read_csv(filename, delimiter=" ", header=0)
        df.rename(columns={"#src": "src"}, inplace=True)
        start_time = df["start_time"].min()
        flow_size = df["flow_size"].min()
        opt_flow_duration = round((8 * flow_size) / bw)
        virtual_rings[ring_id] = {
            "servers": df[df["src_tor"] != df["dst_tor"]][["src", "dst"]].values.tolist(),
            "tors": df["src_tor"].unique().tolist(),
            "start_time": start_time,
            "flow_size": flow_size,
            "opt_end_time": start_time + opt_flow_duration,
        }

    return dict(sorted(virtual_rings.items(), key=lambda item: item[1]["start_time"]))


def get_ring_ids(
    level: int,
    tors: int,
    bw: int,
    load: float,
    link_failure_rate: float,
    parallel: DistributedTraining,
) -> list:
    ring_ids_dir = osp.join(
        getcwd(),
        "experiments",
        parallel.value,
        f"{level}_level",
        f"{tors}_tors",
        f"bw_{bw}",
        f"load_{load}",
        f"link_failure_rate_{link_failure_rate}",
        "greedy",
    )

    ring_ids = []
    for filename in listdir(ring_ids_dir):
        if filename.startswith("job_"):
            ring_id = int(filename.split("-")[0].split("_")[-1])
            ring_ids.append(ring_id)

    return sorted(ring_ids)


def group_virtual_rings(virtual_rings: dict) -> list:
    def intersects(t1, t2):
        """Check if t1 and t2 intersect."""
        return max(t1[0], t2[0]) < min(t1[1], t2[1])

    # Sort tuples by their first element
    data = {k: (v["start_time"], v["opt_end_time"]) for k, v in virtual_rings.items()}
    sorted_items = sorted(data.items(), key=lambda x: x[1][0])

    groups = []
    for key1, val1 in sorted_items:
        ring_ids = set()
        for key2, val2 in sorted_items:
            if key1 != key2 and intersects(val1, val2):
                ring_ids.add(key2)
        group_rings = {}
        for ring_id in ring_ids:
            group_rings[ring_id] = virtual_rings[ring_id]
        groups.append(group_rings)

    return groups
