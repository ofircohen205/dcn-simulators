import random
from mlu_lb.schemas.models import LlmModels
from typing import List
from mlu_lb.schemas.job import Job, Pipeline, DataParallel
from mlu_lb.schemas.machines import Machine
from mlu_lb.utils.units import BILLION, FLOAT32, TERA_FLOP

import math

LOWER_BOUND = 1_000_000_000  # 1s
UPPER_BOUND = 10 * LOWER_BOUND  # 10s


def build_data_parallel_dnn_training_job(
    machine: Machine,
    servers_under_tor: dict,
    num_hosts_under_tor: int,
    job_id: int,
    num_hosts: int,
):
    assigned_hosts: list = []
    while num_hosts > len(assigned_hosts):
        selected_tor = random.sample(servers_under_tor.keys(), 1)[0]
        selected_servers = servers_under_tor.pop(selected_tor)
        if num_hosts - len(assigned_hosts) < num_hosts_under_tor:
            subset = list(selected_servers)[: num_hosts - len(assigned_hosts)]
            assigned_hosts.extend(subset)
            servers_under_tor[selected_tor] = selected_servers.difference(subset)
        else:
            assigned_hosts.extend(list(selected_servers))
    data_parallel_flow_size = math.ceil(144 * BILLION * FLOAT32 / num_hosts)
    model_flops = 19.6 * BILLION
    machine_flops = machine.flops * TERA_FLOP
    compute_time = math.ceil(model_flops / machine_flops) * 1_000_000_000
    return Job(
        job_id=job_id,
        model=None,
        pipelines=[],
        data_parallels=[DataParallel(hosts=assigned_hosts, flow_size=data_parallel_flow_size)],
        start_time=random.randint(LOWER_BOUND, UPPER_BOUND) + compute_time,
        compute_time=compute_time,
        mini_batch_size=0,
    )


def build_2d_dnn_training_job(
    machine: Machine,
    model: LlmModels,
    servers_under_tor: dict,
    job_id: int,
    num_pipelines: int,
):
    pipeline_dedicated_hosts = model.value.get_dedicated_hosts(machine)
    dedicated_hosts = pipeline_dedicated_hosts * num_pipelines
    pipeline_flow_size = model.value.get_pipeline_flow_size()
    pipeline_compute_time = model.value.get_pipeline_stage_compute_time(machine)
    pipelines: List[Pipeline] = []
    assigned_hosts: list = []
    non_assigned_hosts: list = []
    while dedicated_hosts > len(assigned_hosts):
        selected_tor = random.sample(servers_under_tor.keys(), 1)[0]
        selected_servers = list(servers_under_tor.pop(selected_tor))
        if len(non_assigned_hosts) > 0:
            subset = (
                non_assigned_hosts
                + selected_servers[: pipeline_dedicated_hosts - len(non_assigned_hosts)]
            )
            pipelines.append(
                Pipeline(
                    hosts=subset,
                    flow_size=pipeline_flow_size,
                    compute_time=pipeline_compute_time,
                )
            )
            assigned_hosts.extend(subset)
            selected_servers = selected_servers[
                pipeline_dedicated_hosts - len(non_assigned_hosts) :
            ]
            non_assigned_hosts = []
        pipeline_hosts = [
            selected_servers[i : i + pipeline_dedicated_hosts]
            for i in range(0, len(selected_servers), pipeline_dedicated_hosts)
        ]
        for hosts_in_pipeline in pipeline_hosts:
            if len(assigned_hosts) >= dedicated_hosts:
                selected_servers = selected_servers[selected_servers.index(hosts_in_pipeline[0]) :]
                servers_under_tor[selected_tor] = selected_servers
                break
            if len(hosts_in_pipeline) < pipeline_dedicated_hosts:
                non_assigned_hosts.extend(hosts_in_pipeline)
                continue
            pipelines.append(
                Pipeline(
                    hosts=hosts_in_pipeline,
                    flow_size=pipeline_flow_size,
                    compute_time=pipeline_compute_time,
                )
            )
            assigned_hosts.extend(hosts_in_pipeline)

    data_parallels: List[DataParallel] = []
    data_parallel_flow_size = model.value.get_all_reduce_flow_size(
        machine=machine, num_pipelines=num_pipelines
    )
    stages_hosts: dict = {i: [] for i in range(pipeline_dedicated_hosts)}
    for pipeline in pipelines:
        for i, host in enumerate(pipeline.hosts):
            stages_hosts[i].append(host)
    for stage_hosts in stages_hosts.values():
        data_parallels.append(DataParallel(hosts=stage_hosts, flow_size=data_parallel_flow_size))

    compute_time = pipeline_compute_time * dedicated_hosts
    return Job(
        job_id=job_id,
        model=model,
        pipelines=pipelines,
        data_parallels=data_parallels,
        start_time=random.randint(LOWER_BOUND, UPPER_BOUND) + compute_time,
        compute_time=compute_time,
        mini_batch_size=model.value.mini_batch_size,
    )
