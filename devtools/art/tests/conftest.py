# The art tests import the pipeline's own modules (kitlib, kits) from devtools/art, as build.py does.
# Copyright 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later.
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
