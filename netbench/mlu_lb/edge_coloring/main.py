import typer

from mlu_lb.edge_coloring.basic_algorithm import basic_edge_coloring
from mlu_lb.edge_coloring.fast_algorithm import fast_edge_coloring
from mlu_lb.ipc.pool import SharedMemoryPool

from mlu_lb.utils.graph_loader import (
    construct_directed_multigraph,
    construct_bipartite_multigraph,
)

app = typer.Typer()


@app.command()
def edge_coloring(netbench_run_dir: str):
    shared_memory = SharedMemoryPool.get(seed=42, netbench_run_dir=netbench_run_dir)
    response = shared_memory.read()
    shared_memory.close()
    src_dst_pairs = eval(response["src_dst_pairs"])
    fast_algorithm = response["fast_edge_coloring"] == "true"
    output_folder = response["output_folder"]
    directed_multigraph = construct_directed_multigraph(src_dst_pairs=src_dst_pairs)
    bipartite_multigraph = construct_bipartite_multigraph(graph=directed_multigraph)
    if fast_algorithm:
        assignments = fast_edge_coloring(src_dst_pairs, output_folder)
    else:
        assignments = basic_edge_coloring(bipartite_multigraph)
    shared_memory.write(assignments)


if __name__ == "__main__":
    app()
