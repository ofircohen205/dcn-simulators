from dataclasses import dataclass
from mlu_lb.schemas.models import LlmModels
from typing import List, Optional


@dataclass(frozen=True)
class Pipeline:
    hosts: list
    flow_size: int
    compute_time: int


@dataclass(frozen=True)
class DataParallel:
    hosts: list
    flow_size: int


@dataclass(frozen=True)
class Job:
    job_id: int
    model: Optional[LlmModels]
    pipelines: List[Pipeline]
    data_parallels: List[DataParallel]
    start_time: int
    compute_time: int
    mini_batch_size: int
