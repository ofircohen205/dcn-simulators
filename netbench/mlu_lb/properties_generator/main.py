from mlu_lb.schemas.distributed_training import DistributedTraining
from mlu_lb.properties_generator.properties_creator import create_tmp_properties_file

from typer import Typer

from mlu_lb.schemas.routing import Routing

app = Typer()


def create_file(
    level: int,
    tors: int,
    distributed_training: DistributedTraining,
    bandwidth: int,
    load: float,
    link_failure_rate: float,
    routing: Routing,
):
    assert level == 2 or level == 3, "level must be 2 or 3"
    cores = tors // 2
    servers = tors * cores
    create_tmp_properties_file(
        bandwidth=bandwidth,
        distributed_training=distributed_training,
        tors=tors,
        cores=cores,
        servers=servers,
        level=level,
        load=load,
        link_failure_rate=link_failure_rate,
        routing=routing,
    )


@app.command()
def mcvlc(
    level: int,
    tors: int,
    distributed_training: DistributedTraining,
    bandwidth: int,
    load: float,
    link_failure_rate: float,
):
    create_file(
        level=level,
        tors=tors,
        distributed_training=distributed_training,
        bandwidth=bandwidth,
        load=load,
        link_failure_rate=link_failure_rate,
        routing=Routing.MCVLC,
    )


@app.command()
def ecmp(
    level: int,
    tors: int,
    distributed_training: DistributedTraining,
    bandwidth: int,
    load: float,
    link_failure_rate: float,
):
    create_file(
        level=level,
        tors=tors,
        distributed_training=distributed_training,
        bandwidth=bandwidth,
        load=load,
        link_failure_rate=link_failure_rate,
        routing=Routing.ECMP,
    )


@app.command()
def fast_edge_coloring(
    level: int,
    tors: int,
    distributed_training: DistributedTraining,
    bandwidth: int,
    load: float,
    link_failure_rate: float,
):
    create_file(
        level=level,
        tors=tors,
        distributed_training=distributed_training,
        bandwidth=bandwidth,
        load=load,
        link_failure_rate=link_failure_rate,
        routing=Routing.FAST_EDGE_COLORING,
    )


@app.command()
def greedy_edge_coloring(
    level: int,
    tors: int,
    distributed_training: DistributedTraining,
    bandwidth: int,
    load: float,
    link_failure_rate: float,
):
    create_file(
        level=level,
        tors=tors,
        distributed_training=distributed_training,
        bandwidth=bandwidth,
        load=load,
        link_failure_rate=link_failure_rate,
        routing=Routing.GREEDY_EDGE_COLORING,
    )


if __name__ == "__main__":
    app()
