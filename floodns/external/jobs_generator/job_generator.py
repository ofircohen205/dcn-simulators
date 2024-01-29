import math
import random

from external.schemas.accelerators import Accelerator
from external.schemas.job import Job, DataParallel
from external.schemas.models import LlmModels, Model
from external.utils.units import BILLION

random.seed(1234)

LOWER_BOUND = 1_000_000_000  # 1s
UPPER_BOUND = 10 * LOWER_BOUND  # 10s
NUM_NICS_PER_HOST = 8


def create_jobs(
    num_concurrent_jobs: int,
    tor_to_nics: dict,
    accelerator: Accelerator,
    radix: int,
    data_parallelism_dim: int,
):
    num_remaining_nics = sum([len(nics) for nics in tor_to_nics.values()])
    jobs: list = []
    job_id, jobs_total_nics = 0, 0
    while job_id < num_concurrent_jobs:
        if len(tor_to_nics) == 0:
            print("No more NICs to schedule")
            break
        retry = 0
        model = random.choice(list(LlmModels)).value
        num_gpus = model.full_copy * data_parallelism_dim
        job = build_ddp_job(
            model=model,
            accelerator=accelerator,
            job_id=job_id,
            radix=radix,
            tor_to_nics=tor_to_nics,
            data_parallelism_dim=data_parallelism_dim,
        )
        jobs.append(job)
        job_id += 1
        jobs_total_nics += num_gpus
        num_remaining_nics = sum([len(nics) for nics in tor_to_nics.values()])

    return jobs


def build_ddp_job(
    model: Model,
    accelerator: Accelerator,
    job_id: int,
    radix: int,
    tor_to_nics: dict,
    data_parallelism_dim: int,
):
    full_copies = []
    for i in range(data_parallelism_dim):
        full_copy = model.full_copy
        nics = []
        total_nics_to_schedule = NUM_NICS_PER_HOST * math.ceil(full_copy / NUM_NICS_PER_HOST)
        while total_nics_to_schedule > 0:
            tor_id = random.choice(list(tor_to_nics.keys()))
            tor_nics = tor_to_nics[tor_id]
            if total_nics_to_schedule < radix:
                assert (radix - total_nics_to_schedule) % NUM_NICS_PER_HOST == 0
                remaining_tor_nics = list(tor_nics)[:total_nics_to_schedule]
                nics.extend(remaining_tor_nics)
                tor_to_nics[tor_id] = set(list(tor_nics)[total_nics_to_schedule:])
            else:
                nics.extend(list(tor_nics))
                tor_to_nics.pop(tor_id)
            total_nics_to_schedule = total_nics_to_schedule - len(tor_nics)

        if len(nics) > full_copy:
            nics = nics[:full_copy]
        full_copies.append(nics)

    data_parallels = []
    for i in range(model.full_copy):
        ring_nics = [full_copies[j][i] for j in range(len(full_copies))]
        flow_size = math.ceil(
            math.ceil(BILLION * model.weights / model.full_copy) / data_parallelism_dim
        )
        data_parallels.append(DataParallel(nics=ring_nics, flow_size=flow_size))

    compute_time = model.get_compute_time(accelerator=accelerator)
    return Job(
        job_id=job_id,
        model=model,
        pipelines=[],
        data_parallels=data_parallels,
        start_time=random.randint(LOWER_BOUND, UPPER_BOUND) + compute_time,
        compute_time=compute_time,
        mini_batch_size=0,
    )
