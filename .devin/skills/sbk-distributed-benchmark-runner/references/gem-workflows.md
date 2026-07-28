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

Verify the host key out of band before adding it to `known_hosts`. Passwordless
public-key authentication through the SSH agent or configured identity files is
preferred. If password authentication is required, supply `-gempass` at runtime
or `SBK_GEM_SSH_PASSWD`; do not store it in YML or source control.

Also verify:

- remote account can create/write the GEM working directory;
- enough space exists if Java or SBK must be copied;
- remote Java is compatible, or Java copying is deliberately enabled;
- the local `-sbkdir` contains the distribution to verify/copy;
- the remote node reaches both storage and advertised SBM callback address.

## Minimal GEM plumbing smoke test

`Null` isolates SSH, deployment, launch, callback, and aggregation from storage:

```bash
./build/install/sbk/bin/sbk-gem \
  -nodes loadgen-a.example.com,loadgen-b.example.com \
  -gemuser sbk \
  -localhost gem-controller.example.com \
  -sbkdir "$PWD/build/install/sbk" \
  -class null -writers 1 -size 100 -records 10000 -time ns
```

It is not a storage benchmark. Replace names only after checking the target
inventory and SSH trust.

## Deployment controls

| Option | Meaning |
|---|---|
| `-copy true` | Copy local SBK when remote expected version is missing/mismatched |
| `-delete true` | Remove a mismatched remote SBK before replacement |
| `-deleteafter false` | Keep reconciled remote SBK after the run for reuse |
| `-javaversion 25` | Required remote Java major version |
| `-javacopy true` | Copy the controller JVM when required Java is unavailable |
| `-javadir <home>` | Optional remote Java home containing `bin/java` |

Defaults come from generated help and may change. Copying Java is valid only
when the controller JVM satisfies `-javaversion`. GEM verifies and exports an
absolute node-specific `SBK_JAVA_HOME` before running remote SBK.

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
