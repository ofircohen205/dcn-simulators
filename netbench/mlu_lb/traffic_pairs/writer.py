from mlu_lb.schemas.job import Job
from mlu_lb.utils.graph_loader import get_tor_of_host
import networkx as nx

HEADER = "#src dst src_tor dst_tor start_time flow_size compute_time stage_index\n"
LINE = "{src} {dst} {src_tor} {dst_tor} {start_time} {flow_size} {compute_time} {stage_index}\n"


def write_job_virtual_links_file(graph: nx.Graph, pp_filename: str, dp_filename: str, job: Job):
    write_pipeline_virtual_links_file(graph, pp_filename, job)
    write_data_parallel_virtual_links_file(graph, dp_filename, job)


def write_pipeline_virtual_links_file(graph: nx.Graph, pp_filename: str, job: Job):
    with open(pp_filename, "w") as f:
        f.write(HEADER)
        for pipeline in job.pipelines:
            for worker, next_worker in zip(pipeline.hosts[:-1], pipeline.hosts[1:]):
                f.write(
                    LINE.format(
                        src=worker,
                        src_tor=get_tor_of_host(graph=graph, host_id=worker),
                        dst=next_worker,
                        dst_tor=get_tor_of_host(graph=graph, host_id=next_worker),
                        start_time=job.start_time,
                        flow_size=pipeline.flow_size,
                        compute_time=pipeline.compute_time,
                        stage_index=0,
                    )
                )


def write_data_parallel_virtual_links_file(graph: nx.Graph, dp_filename: str, job: Job):
    with open(dp_filename, "w") as f:
        f.write(HEADER)
        for stage, data_parallel in enumerate(job.data_parallels):
            for worker, next_worker in zip(data_parallel.hosts[:-1], data_parallel.hosts[1:]):
                f.write(
                    LINE.format(
                        src=worker,
                        src_tor=get_tor_of_host(graph=graph, host_id=worker),
                        dst=next_worker,
                        dst_tor=get_tor_of_host(graph=graph, host_id=next_worker),
                        start_time=job.start_time,
                        flow_size=data_parallel.flow_size,
                        compute_time=job.compute_time,
                        stage_index=stage,
                    )
                )
            f.write(
                LINE.format(
                    src=data_parallel.hosts[-1],
                    src_tor=get_tor_of_host(graph=graph, host_id=data_parallel.hosts[-1]),
                    dst=data_parallel.hosts[0],
                    dst_tor=get_tor_of_host(graph=graph, host_id=data_parallel.hosts[0]),
                    start_time=job.start_time,
                    flow_size=data_parallel.flow_size,
                    compute_time=job.compute_time,
                    stage_index=stage,
                )
            )
