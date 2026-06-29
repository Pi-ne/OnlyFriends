"""Shared helpers for all routers."""

import time
from typing import TypeVar

from ..models import Result

T = TypeVar("T")


def success(data: T) -> Result[T]:
    """Wrap data in the standard Result envelope matching the Java contract."""
    return Result(
        code=200,
        message="success",
        data=data,
        timestamp=int(time.time() * 1000),
    )
