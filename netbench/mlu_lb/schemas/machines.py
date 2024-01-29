from enum import Enum
from dataclasses import dataclass


@dataclass(frozen=True)
class Machine:
    machine: str  # machine name
    memory: int  # memory in GB
    flops: float  # TeraFLOPS


class Machines(Enum):
    TPU_V4 = Machine(machine="TPU-v4", memory=32, flops=260.0)
    A100 = Machine(machine="A100", memory=80, flops=312.0)
    H100 = Machine(machine="H100", memory=80, flops=1979.0)
    AWS_TRAINIUM_V1 = Machine(machine="AWS-Trainium-v1", memory=512, flops=3400.0)
    AWS_TRAINIUM_V2 = Machine(machine="AWS-Trainium-v2", memory=512, flops=3400.0)
    ASCEND_910 = Machine(machine="Ascend-910", memory=32, flops=320.0)


machine_names = [m.name for m in Machines]
