# Standalone SBM workflow

Use standalone SBM when clients are launched independently instead of through
SBK-GEM.

## Start the aggregator

SBM requires a storage label and action describing the incoming client
workload. Generate help with the class present:

```bash
./sbm/build/install/sbm/bin/sbm -class file -help
```

Start a console/Prometheus aggregate:

```bash
./sbm/build/install/sbm/bin/sbm \
  -class file -action w -port 9717 -max 2
```

Or use the built-in Local Web Console:

```bash
./sbm/build/install/sbm/bin/sbm \
  -class file -action w -port 9717 -max 2 \
  -out SbmWebLogger -webopen false
```

`SBM` performs no storage operations. Keep it running while clients connect.
The `-class` and `-action` values label and configure aggregation; they must
match the intended workload.

## Launch clients

On each load host, use the same compatible SBK distribution and direct
measurements to the aggregator:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-agent-a.dat \
  -writers 1 -size 4096 -seconds 30 -time mcs \
  -out GrpcLogger -sbm sbm-controller.example.com -sbmport 9717
```

Generate authoritative GrpcLogger options:

```bash
./build/install/sbk/bin/sbk \
  -class <driver> -out GrpcLogger -help
```

Launch all clients only after confirming that each host can connect to the SBM
address. A firewall allowing controller-to-client traffic does not prove the
required client-to-controller callback path.

## Validate

- SBM connections must equal the expected number of client processes.
- Client driver, action, record size, latency unit, and protocol version must
  be compatible.
- Preserve every client log as well as SBM aggregate output.
- Compare the sum of client completions with aggregate totals.
- A clean SBM total does not override a client-side I/O failure; inspect both.
- Stop clients first, then stop SBM after final records and disconnects arrive.

For normal GEM execution, do not use this workflow: GEM starts and owns its
embedded SBM automatically.
