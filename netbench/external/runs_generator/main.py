import os
from os import getcwd, makedirs

from typer import Typer

from external.schemas.routing import Routing

app = Typer()

NUM_CORE_FAILURES = [0, 1, 4, 8]
RING_SIZES = [2, 4, 8]


@app.command()
def create_concurrent_jobs_dir(num_tors: int, num_concurrent_jobs: int):
    for num_cores in NUM_CORE_FAILURES:
        for ring_size in RING_SIZES:
            for routing in Routing:
                job_dir = os.path.join(
                    getcwd(),
                    "experiments",
                    f"concurrent_jobs_{num_concurrent_jobs}",
                    f"{num_cores}_core_failures",
                    f"ring_size_{ring_size}",
                    routing.value,
                )
                if not os.path.exists(job_dir):
                    makedirs(job_dir, exist_ok=True)
                traffic_pairs_dir = os.path.join(
                    getcwd(),
                    "traffic_pairs",
                    f"concurrent_jobs_{num_concurrent_jobs}",
                    f"ring_size_{ring_size}",
                )
                create_files(
                    runs_dir=job_dir,
                    routing=routing,
                    num_cores=num_cores,
                    traffic_pairs_dir=traffic_pairs_dir,
                    num_tors=num_tors,
                )


def create_files(
    runs_dir: str,
    routing: Routing,
    num_cores: int,
    traffic_pairs_dir: str,
    num_tors: int,
):
    radix = num_tors // 2
    num_servers = num_tors * radix
    with open(os.path.join(runs_dir, f"{routing.value}.properties"), "w") as f:
        f.write("# Topology\n")
        f.write(
            f"scenario_topology_file=topologies/fat_tree/2_level/fat_tree-{num_tors}_tors-{radix}_cores-{num_servers}_servers.topology\n"
        )
        f.write("# Run info\n")
        f.write(f"run_seed=1234\n")
        f.write(f"run_time_s=604800\n")
        f.write(f"run_folder_name={routing.value}\n")
        f.write(f"run_folder_base_dir={runs_dir}\n")
        f.write("# Flow scheduling project info\n")
        f.write(f"routing_scheme={routing.value}\n")
        f.write("generate_flow_after_finished=false\n")
        f.write("# Flow size estimation project info\n")
        f.write("estimate_flow_size=false\n")
        f.write(
            "estimate_flow_size_model_path=data/fat_tree-16_tors-16_cores-512_servers.topology\n"
        )
        f.write("# Network device\n")
        f.write("transport_layer=simple_dctcp\n")
        f.write("network_device=ecmp_switch\n")
        f.write("network_device_routing=ecmp\n")
        f.write("network_device_intermediary=identity\n")
        f.write("# Link & output port\n")
        f.write("output_port=ecn_tail_drop\n")
        f.write("output_port_max_queue_size_bytes=64000000\n")
        f.write("output_port_ecn_threshold_k_bytes=1500000\n")
        f.write("link=perfect_simple\n")
        f.write("link_delay_ns=10\n")
        f.write("link_bandwidth_bit_per_ns=100\n")
        f.write("# Traffic\n")
        f.write("traffic=traffic_pair\n")
        f.write("traffic_pair_type=data_parallel\n")
        f.write(f"base_traffic_pairs_dir={traffic_pairs_dir}\n")
        f.write("epochs=10\n")
        f.write(f"num_failed_nodes={num_cores}\n")


if __name__ == "__main__":
    app()
