import random
from mlu_lb.schemas.models import LlmModels
from mlu_lb.schemas.machines import Machine
from mlu_lb.traffic_pairs.job_generator import (
    build_2d_dnn_training_job,
    build_data_parallel_dnn_training_job,
)

PIPELINE = 0
HYBRID = 1
NUM_ACCELERATORS_PER_HOST = 8


def place_data_parallelism_jobs(servers_under_tor: dict, machine: Machine, load: float) -> list:
    """
    Generate DNN training jobs with data parallelism.
    :param servers_under_tor: dict of servers under each ToR
    :param machine: machine to run the jobs
    :param load: load of the jobs
    :return: list of jobs
    """
    total_hosts = round(sum(len(servers) for servers in servers_under_tor.values()) * load)
    num_hosts_under_tor = round(total_hosts / len(servers_under_tor))
    print(f"Total hosts: {total_hosts}, load: {load}")

    jobs: list = []
    job_id, jobs_total_hosts = 0, 0
    exceeded_hosts = False
    while jobs_total_hosts < total_hosts:
        retry = 0
        num_hosts = random.randint(4, 32)
        while num_hosts > total_hosts - jobs_total_hosts:
            if retry > 100:
                print("Retry too many times, break")
                exceeded_hosts = True
                break
            num_hosts = random.randint(4, 32)
            retry += 1
        if exceeded_hosts:
            break
        job = build_data_parallel_dnn_training_job(
            machine=machine,
            servers_under_tor=servers_under_tor,
            num_hosts_under_tor=num_hosts_under_tor,
            job_id=job_id,
            num_hosts=num_hosts,
        )
        jobs.append(job)
        job_id += 1
        jobs_total_hosts += num_hosts

    return jobs


def place_hybrid_parallelism_jobs(servers_under_tor: dict, machine: Machine, load: float) -> list:
    """
    Generate DNN training jobs with mixed parallelism (virtual ring and pipelines).
    :param servers_under_tor: dict of servers under each ToR
    :param machine: machine to run the jobs
    :param load: load of the jobs
    :return: list of jobs
    """
    total_hosts = round(sum(len(servers) for servers in servers_under_tor.values()) * load)
    print(f"Total hosts: {total_hosts}, load: {load}")

    models = [model for model in LlmModels]
    models_dedicated_hosts = {
        model.name: model.value.get_dedicated_hosts(machine) for model in LlmModels
    }

    jobs: list = []
    job_id, jobs_total_hosts = 0, 0
    exceeded_hosts = False
    while jobs_total_hosts + min(models_dedicated_hosts.values()) < total_hosts:
        retry = 0
        num_pipelines = random.randint(4, 32)
        model = random.choice(models)
        dedicated_hosts = models_dedicated_hosts[model.name] * num_pipelines
        while dedicated_hosts > total_hosts - jobs_total_hosts:
            if retry > 100:
                print("Retry too many times, break")
                exceeded_hosts = True
                break
            num_pipelines = random.randint(4, 32)
            model = random.choice(models)
            dedicated_hosts = models_dedicated_hosts[model.name] * num_pipelines
            retry += 1
        if exceeded_hosts:
            break
        job = build_2d_dnn_training_job(
            machine=machine,
            model=model,
            servers_under_tor=servers_under_tor,
            job_id=job_id,
            num_pipelines=num_pipelines,
        )
        jobs.append(job)
        job_id += 1
        jobs_total_hosts += sum(map(lambda p: len(p.hosts), job.pipelines))

    return jobs
