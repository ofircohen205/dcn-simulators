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
                    "runs",
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
    create_config_floodns(
        root=runs_dir,
        routing=routing,
        num_cores=num_cores,
        traffic_pairs_dir=traffic_pairs_dir,
    )
    create_2_layer_topology(root=runs_dir, num_tors=num_tors)
    if os.path.exists(os.path.join(runs_dir, "schedule.csv")):
        return
    with open(os.path.join(runs_dir, "schedule.csv"), "w") as f:
        f.write("0,5,8,100000000,0,,\n")


def create_config_floodns(
    root: dir,
    routing: Routing,
    num_cores: int,
    traffic_pairs_dir: str,
):
    config_file = os.path.join(root, "config.properties")
    if os.path.exists(config_file):
        print(f"config file already exists... {config_file}")
        return

    with open(config_file, "w") as f:
        f.write("simulation_end_time_ns=604800000000000\n")
        f.write("simulation_seed=1234\n")
        f.write("filename_topology=topology.properties\n")
        f.write("filename_schedule=schedule.csv\n")
        f.write(f"job_base_dir_schedule={traffic_pairs_dir}\n")
        f.write(f"routing_strategy={routing.value}\n")
        f.write(f"num_failed_nodes={num_cores}\n")


def create_2_layer_topology(root: dir, num_tors: int):
    topology_file = os.path.join(root, "topology.properties")
    radix = num_tors // 2
    num_cores = radix
    num_hosts_under_tor = radix
    num_hosts = num_tors * num_hosts_under_tor
    num_switches = num_tors + num_cores
    num_nodes = num_switches + num_hosts
    num_edges = num_tors * (num_cores + num_hosts_under_tor)
    undirected_edges = build_2_layer_undirected_edges(
        num_tors=num_tors, num_cores=num_cores, num_hosts=num_hosts, radix=radix
    )
    if os.path.exists(topology_file):
        print(f"topology file already exists... {topology_file}")
        return

    with open(topology_file, "w") as f:
        f.write(f"num_nodes={num_nodes}\n")
        f.write(f"num_undirected_edges={num_edges}\n")
        f.write(f"switches=incl_range(0,{num_switches - 1})\n")
        f.write(f"switches_which_are_tors=incl_range(0,{num_tors - 1})\n")
        f.write(f"cores=incl_range({num_tors},{num_switches - 1})\n")
        f.write(f"servers=incl_range({num_switches},{num_switches + num_hosts - 1})\n")
        f.write(f"undirected_edges=incl_range({undirected_edges})\n")
        f.write(f"link_data_rate_bit_per_ns=100\n")


def build_2_layer_undirected_edges(num_tors: int, num_cores: int, num_hosts: int, radix: int):
    start_tor, end_tor = 0, num_tors - 1
    start_core, end_core = num_tors, num_tors + num_cores - 1
    start_server, end_server = num_tors + num_cores, num_tors + num_cores + num_hosts - 1
    tor_core_edges = f"{start_tor}:{end_tor}-{start_core}:{end_core}"
    server_tor_edges = []
    tor = start_tor
    for server in range(start_server, end_server + 1, radix):
        server_tor_edges.append(f"{server}:{server + radix - 1}-{tor}")
        tor += 1
    return f"{tor_core_edges},{','.join(server_tor_edges)}"


if __name__ == "__main__":
    app()
