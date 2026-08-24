# SBK-GEM and SBK-GEM-YAL workflows

## Discover current options

```bash
./build/install/sbk/bin/sbk-gem -help
./build/install/sbk/bin/sbk-gem -class <driver> -help
./build/install/sbk/bin/sbk-gem -class <driver> -out GemWebLogger -help
./build/install/sbk/bin/sbk-gem-yal -help
```

GEM separates its orchestration options and forwards driver/SBK options to
remote SBK. The remote command is forced to use `GrpcLogger`; aggregate output
is produced locally.

## SSH preflight

For every node, under the exact local account running GEM:

```bash
ssh -o BatchMode=yes <gem-user>@<node> true
```

This command validates passwordless public-key or SSH-agent authentication
only: `BatchMode=yes` disables password prompting. It does not validate a
password later supplied through `-gempass` or `SBK_GEM_SSH_PASSWD`.

Verify the host key out of band before adding it to `known_hosts`. Passwordless
public-key authentication through the SSH agent or configured identity files is
preferred. If password authentication is required, supply `-gempass` at runtime
or `SBK_GEM_SSH_PASSWD`; do not store it in YML or source control.

Also verify:

- remote account can create/write the GEM working directory;
- enough space exists if Java or SBK must be copied;
- remote Java is the controller major version or newer, or enough space exists
  for GEM to provision the controller JDK automatically;
- the generated launcher resolves a complete local distribution through its
  internal `sbk.appHome` property;
- the remote node reaches both storage and advertised SBM callback address.

## Minimal GEM plumbing smoke test

`Null` isolates SSH, deployment, launch, callback, and aggregation from storage:

```bash
./build/install/sbk/bin/sbk-gem \
  -nodes loadgen-a.example.com,loadgen-b.example.com \
  -gemuser sbk \
  -localhost gem-controller.example.com \
  -class null -writers 1 -size 100 -records 10000 -time ns
```

It is not a storage benchmark. Replace names only after checking the target
inventory and SSH trust.

## Deployment controls

| Option | Meaning |
|---|---|
| `-runtimecleanup true` | Retire inactive non-current GEM-managed SBK runtimes while retaining current and leased identities |
| `-javadir <home>` | Optional remote Java home containing `bin/java` |

Defaults come from generated help and may change. GEM always deploys the SBK
distribution selected internally by the generated launcher's `sbk.appHome`.
It reuses a same-or-newer remote Java or copies the controller JDK when needed,
then verifies an absolute node-specific Java home before running remote SBK.

## GEM-YAL

YML uses the top-level `sbkGemArgs` map:

```bash
./build/install/sbk/bin/sbk-gem-yal \
  -f .devin/skills/sbk-distributed-benchmark-runner/references/example-sbk-gem-null-smoke.yml
```

Command-line values override YML values:

```bash
./build/install/sbk/bin/sbk-gem-yal \
  -f /secure/runtime/benchmark.yml \
  -seconds 30 -writers 2
```

Use `sbk-gem-yal -p -f <file>` to print effective GEM/SBK option help. Keep
secrets and sensitive host inventories in an untracked runtime file or an
approved secret-injection system.

## Add the real backend

After the Null smoke succeeds:

1. run the exact ordinary SBK backend command on one load node;
2. add the same driver options to GEM;
3. use one GEM node and a small fixed `-records` test;
4. verify data and aggregate counts;
5. run a short `-seconds` test;
6. add nodes gradually while checking connections and per-node returns.

The MinIO template is illustrative. Generate
`sbk-gem -class minio -help` and read `drivers/minio/README.md` before use.
