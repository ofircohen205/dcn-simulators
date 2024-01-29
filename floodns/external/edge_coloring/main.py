import typer

from external.edge_coloring.edge_coloring import color_edges
from external.ipc.pool import SharedMemoryPool
from external.utils.ipc import fetch_commodities

app = typer.Typer()


@app.command()
def edge_coloring(run_dir: str):
    shared_memory = SharedMemoryPool.get(seed=42, run_dir=run_dir)
    response = shared_memory.read()
    shared_memory.close()
    commodities = fetch_commodities(response["src_dst_pairs"])
    # output_folder = response["output_folder"]
    # assignments = fast_edge_coloring(commodities=commodities, output_folder=output_folder)

    assignments = color_edges(commodities=commodities)

    shared_memory.write(assignments)


if __name__ == "__main__":
    app()
