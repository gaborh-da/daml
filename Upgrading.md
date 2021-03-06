# How to upgrade packages in this repo

## Stackage

To update the Stackage snapshot you need to clone the hazel repository into a
separate directory:

```
$ git clone https://github.com/tweag/rules_haskell.git
```

Change into the hazel directory.

```
$ cd rules_haskell/hazel
```

Then execute the following command to update to the specified Stackage
snapshot, where `$PROJECT` points to the root of this repository:
(Requires `stack`)

```
$ Stackage.hs lts-12.4 "$PROJECT/hazel/packages.bzl"
```

On NixOS you may need to modify `Stackage.hs` to append the following flag to
the list of `stack` intrepreter flags: `--nix-packages zlib`.

This will take a while.


## Nixpkgs

To update the nixpkgs revision, go to the `nix/nixpkgs` directory in
this repository and run `nix-update-src-json ./nixos-*` to update to
the latest release in the current NixOS channel. To upgrade to a new
channel move `nixos-18.09` to `nixos-19.03` (adapt to the channels you
want to move from and to), change the `branch` field in
`default.src.json` and modify `nix/nixpkgs.nix` to point to the new
channel.

After upgrading the revision, the easiest solution is usually to open
a PR and see what fails on CI (running the builds locally can take
quite some time). The most common reason for failures is usually that
we have overriden a specific package with certain patches that no
longer work. In that case, a good first step is to check if these
patches are still necessary and if not try to switch to the unpatched
package from nixpkgs.
