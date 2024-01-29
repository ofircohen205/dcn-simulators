from enum import Enum
from dataclasses import dataclass

from mlu_lb.schemas.machines import Machine
from mlu_lb.utils.units import BILLION, TRILLION, BFLOAT16, FLOAT32, TERA_FLOP

import math

NUM_ACCELERATORS_PER_HOST = 8
MICRO_BATCH_SIZE = 8

"""
Model Papers:
    - GPT-3: https://arxiv.org/pdf/2005.14165.pdf
    - OPT-175B: https://arxiv.org/pdf/2205.01068.pdf
    - PaLM: https://arxiv.org/pdf/2204.02311.pdf
    - LLaMA2-70B: https://arxiv.org/pdf/2307.09288.pdf
    - Gopher: https://arxiv.org/pdf/2112.11446.pdf
    - MT-NLG: https://arxiv.org/pdf/2201.11990.pdf
    - PanGu-Sigma: https://arxiv.org/pdf/2303.10845.pdf
"""


@dataclass(frozen=True)
class Model:
    name: str  # model name
    size: float  # model size (parameters) in billions
    num_layers: int
    global_batch_size: int  # number of tokens
    context_window: int
    num_tokens: float  # number of tokens (for language models)
    hidden_dim: int

    def get_model_size_gb(self) -> float:
        r"""
        Baseline model size:
        .. math::
            (2 + 2 + 12) * \Psi

        Optimizer State Partitioning:

        .. math::
            2 * \Psi + 2 * \Psi + \frac{12 * \Psi}{N_d}

        Gradient Partitioning:

        .. math::
            2 * \Psi + \frac{(2 + 12) * \Psi}{N_d}

        Parameter Partitioning:

        .. math::
            \frac{(2 + 2 + 12) * \Psi}{N_d}

        where :math:`\Psi` is the number of parameters in billions,
        and :math:`N_d` denotes DP degree.

        :return: model size in GB (based on the baseline model)
        """
        return (self.weights + self.gradients + self.optimizer_state) / BILLION

    def get_activations_size_gb(self) -> float:
        return MICRO_BATCH_SIZE * self.context_window * self.hidden_dim * self.num_layers * BFLOAT16

    def get_model_flops(self) -> float:
        return 6 * self.size * self.num_tokens

    def get_layer_flops(self) -> float:
        return self.get_model_flops() / self.num_layers

    def get_pipeline_flow_size(self) -> int:
        activations_size = self.get_activations_size_gb()
        return math.ceil(activations_size / self.num_layers)

    def get_all_reduce_flow_size(self, machine: Machine, num_pipelines: int) -> int:
        weights = self.weights
        num_layers_under_host = self.get_num_layers_under_host(machine)
        return math.ceil(weights / (num_layers_under_host * num_pipelines))

    def get_dedicated_hosts(self, machine: Machine) -> int:
        model_size = self.get_model_size_gb()
        host_memory = machine.memory * NUM_ACCELERATORS_PER_HOST
        return math.ceil(model_size / host_memory)

    def get_num_layers_under_host(self, machine: Machine) -> int:
        return math.ceil(self.num_layers / self.get_dedicated_hosts(machine))

    def get_pipeline_stage_compute_time(self, machine: Machine) -> int:
        machine_flops = machine.flops * TERA_FLOP  # in flops
        stage_flops = self.get_num_layers_under_host(machine) * self.get_layer_flops()
        t_computation = math.ceil(stage_flops / machine_flops)
        return t_computation  # in nanoseconds

    @property
    def weights(self):
        return BFLOAT16 * self.size

    @property
    def gradients(self):
        return BFLOAT16 * self.size

    @property
    def optimizer_state(self):
        return 3 * FLOAT32 * self.size

    @property
    def mini_batch_size(self) -> int:
        return self.global_batch_size // self.context_window


class LlmModels(Enum):
    GPT_3 = Model(
        name="GPT_3",
        size=175 * BILLION,
        num_tokens=300 * BILLION,
        num_layers=96,
        global_batch_size=3_200_000,
        context_window=2048,
        hidden_dim=12288,
    )
    OPT_175B = Model(
        name="OPT_175B",
        size=175 * BILLION,
        num_tokens=300 * BILLION,
        num_layers=96,
        global_batch_size=2_000_000,
        context_window=2048,
        hidden_dim=12288,
    )
    PaLM = Model(
        name="PaLM",
        size=540 * BILLION,
        num_tokens=780 * BILLION,
        num_layers=118,
        global_batch_size=2048 * 2240,
        context_window=2048,
        hidden_dim=18432,
    )
    LLAMA2_70B = Model(
        name="LLaMA2_70B",
        size=70 * BILLION,
        num_tokens=2 * TRILLION,
        num_layers=80,
        global_batch_size=2048 * 1024,
        context_window=4096,
        hidden_dim=8192,
    )
    GOPHER = Model(
        name="GOPHER",
        size=280 * BILLION,
        num_tokens=300 * BILLION,
        num_layers=80,
        global_batch_size=6_000_000,
        context_window=2048,
        hidden_dim=16384,
    )
    MT_NLG = Model(
        name="MT_NLG",
        size=530 * BILLION,
        num_tokens=300 * BILLION,
        num_layers=105,
        global_batch_size=4480 * 1920,
        context_window=2048,
        hidden_dim=20480,
    )
    PANGU_SIGMA = Model(
        name="PANGU_SIGMA",
        size=1000 * BILLION,
        num_tokens=329 * BILLION,
        num_layers=96,
        global_batch_size=512 * 512,
        context_window=1024,
        hidden_dim=5120,
    )
