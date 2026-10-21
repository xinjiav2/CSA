---
layout: post
permalink: /AoA/
author: Jade
title: Arch on ARM!
showReadingTime: true
toc: true
---

# Arch Linux ARM on an HP OmniBook X 14 (Snapdragon X), using Ubuntu's kernel

Documentation of a dual-boot setup where Arch Linux ARM's userspace runs on top of the kernel, modules, firmware, and initramfs from a working Ubuntu 26.04 aarch64 install. Ubuntu's GRUB loads the kernel and points its `root=` at the Arch partition.

**Date:** 2026-10-20
**Hardware:** HP OmniBook X 14 (fe0xxx), Qualcomm Snapdragon X, aarch64
**Host:** Ubuntu 26.04, kernel `7.0.0-38-generic`
**Guest:** Arch Linux ARM, `ArchLinuxARM-aarch64-latest.tar.gz` (ARMv8 AArch64 Multi-platform) from <https://archlinuxarm.org/about/downloads>

---

## How it works

The Arch ARM tarball is only a userspace root filesystem. It has no kernel that boots this laptop, so:

| Piece | Comes from |
|---|---|
| Kernel (`vmlinuz`) and initramfs | Ubuntu's `/boot` (on the Ubuntu root partition) |
| Kernel modules | Copied from Ubuntu `/lib/modules/<version>` |
| Firmware (GPU, Wi-Fi, DSPs) | Copied from Ubuntu `/lib/firmware` |
| Userspace, pacman, packages | Arch Linux ARM tarball |
| Bootloader | Ubuntu's GRUB, with a custom menu entry |

The laptop boots via ACPI, so no `devicetree` line is needed in the GRUB entry. This was verified by grepping `grub.cfg` for `devicetree` and `dtb` and finding nothing.

## Disk layout used

| Partition | Purpose | Filesystem |
|---|---|---|
| `nvme0n1p1` | EFI System Partition | vfat |
| `nvme0n1p3`, `p4` | Windows | ntfs |
| `nvme0n1p5` | Ubuntu root (contains `/boot`) | ext4 |
| `nvme0n1p6` | Other | ext4 |
| `nvme0n1p7` | **Arch root** (about 114 GB) | ext4 |

UUIDs used below (replace with yours from `lsblk -f`):

- Ubuntu root (`p5`): `c8b9b28b-eab1-4441-a158-027c42288db5`
- Arch root (`p7`): `ef3b53f1-ab9a-4ca0-a1eb-12b9fa018402`

---

## Step 1: Create the Arch root partition

Shrink Ubuntu or use free space (use a live USB if you need to shrink a mounted root), then create a partition with `cfdisk /dev/nvme0n1`. Format and extract:

```bash
sudo mkfs.ext4 /dev/nvme0n1p7        # double-check the device first, this erases it
sudo mkdir -p /mnt/arch
sudo mount /dev/nvme0n1p7 /mnt/arch
cd ~/Downloads
sudo bsdtar -xpf ArchLinuxARM-aarch64-latest.tar.gz -C /mnt/arch
```

- Use `bsdtar` (`sudo apt install libarchive-tools`), not GNU tar, to avoid xattr and file-capability problems.
- Run it as root with `-p` so permissions are preserved.
- `ls /mnt/arch` should show `bin boot dev etc home usr var ...`.

> **Gotcha:** An early attempt failed because the commands contained placeholders (`nvme0n1pX`, nonexistent `/mnt/arch`). Always substitute your real partition and create the mount point first.

## Step 2: Copy modules and firmware from Ubuntu

```bash
sudo mkdir -p /mnt/arch/usr/lib/modules /mnt/arch/usr/lib/firmware
sudo cp -a /lib/modules/$(uname -r) /mnt/arch/usr/lib/modules/
sudo cp -a /lib/firmware/. /mnt/arch/usr/lib/firmware/
```

The modules must match the kernel version exactly. Firmware is critical on Snapdragon X laptops (GPU, Wi-Fi, remoteprocs).

## Step 3: Collect the boot parameters

```bash
uname -r
ls /boot
cat /proc/cmdline
sudo grep -E "menuentry |devicetree|dtb" /boot/grub/grub.cfg
```

Working Ubuntu command line on this machine:

```
BOOT_IMAGE=/boot/vmlinuz-7.0.0-38-generic root=UUID=c8b9b28b-... ro clk_ignore_unused pd_ignore_unused cma=128M efi=noruntime crashkernel=...
```

The extra parameters (`clk_ignore_unused pd_ignore_unused cma=128M efi=noruntime`) are what make Snapdragon X laptops boot reliably, so they are carried over to the Arch entry. `crashkernel=` is omitted since it only matters for crash dumps.

## Step 4: Chroot setup

Use mount propagation flags so unmounting the chroot cannot affect the host's `/dev`:

```bash
sudo mount --rbind /dev /mnt/arch/dev
sudo mount --make-rslave /mnt/arch/dev
sudo mount -t proc proc /mnt/arch/proc
sudo mount --rbind /sys /mnt/arch/sys
sudo mount --make-rslave /mnt/arch/sys
sudo mount --bind /run /mnt/arch/run
sudo mount --make-slave /mnt/arch/run

# the tarball's resolv.conf points at a resolver that isn't running in the chroot
sudo mv /mnt/arch/etc/resolv.conf /mnt/arch/etc/resolv.conf.bak
sudo cp /etc/resolv.conf /mnt/arch/etc/resolv.conf

sudo chroot /mnt/arch /bin/bash
```

Inside the chroot (the prompt becomes `[root@... /]#`):

```bash
pacman-key --init
pacman-key --populate archlinuxarm
passwd
echo "UUID=ef3b53f1-ab9a-4ca0-a1eb-12b9fa018402 / ext4 defaults 0 1" > /etc/fstab

# keep the generic ARM kernel out
pacman -R linux-aarch64
pacman -Syu networkmanager nano sudo
systemctl enable NetworkManager

# restore the resolver symlink for normal boot
rm /etc/resolv.conf
ln -s ../run/systemd/resolve/stub-resolv.conf /etc/resolv.conf
systemctl enable systemd-resolved

useradd -m -G wheel <yourname>
passwd <yourname>
exit
```

Back on the Ubuntu host, allow `sudo` for the wheel group and clean up:

```bash
sudo sed -i 's/^# %wheel ALL=(ALL:ALL) ALL/%wheel ALL=(ALL:ALL) ALL/' /mnt/arch/etc/sudoers
sudo umount -R /mnt/arch
```

### Gotchas hit during this step

1. **`sudo: command not found`.** The commands were pasted while still inside the chroot, where `sudo` is not installed. Run `exit` to return to the Ubuntu prompt (`jade@...`) before pasting host commands.
2. **`sudo: unable to open pty` on the host.** Plain `--bind` mounts of `/dev` share propagation with the host, so unmounting inside the chroot side removed the host's `/dev/pts`. Fix: reboot (or `pkexec mount -t devpts devpts /dev/pts -o gid=5,mode=620,ptmxmode=666`). The `--make-rslave` mounts above prevent this.
3. **`umount: /mnt/arch/dev: target is busy`.** `pacman-key --init` leaves a `gpg-agent` / `dirmngr` running inside the chroot. Kill them, then unmount again:
   ```bash
   sudo pkill -f 'gpg-agent --homedir /etc/pacman.d/gnupg'
   sudo pkill -f 'dirmngr --homedir /etc/pacman.d/gnupg'
   sudo umount -R /mnt/arch
   ```
4. **A user literally named `yourname` was created**, because a placeholder was pasted as-is. Rename it with:
   ```bash
   sudo usermod -R /mnt/arch -l <realname> -d /home/<realname> -m yourname
   sudo groupmod -R /mnt/arch -n <realname> yourname
   ```

## Step 5: Verify before touching GRUB

Read-only checks from Ubuntu with `/mnt/arch` mounted:

```bash
ls -l /mnt/arch/usr/bin/nmcli /mnt/arch/usr/bin/NetworkManager
ls -l /mnt/arch/etc/systemd/system/multi-user.target.wants/NetworkManager.service
ls -l /mnt/arch/usr/bin/wpa_supplicant
cat /mnt/arch/etc/fstab
ls /mnt/arch/boot                       # should be empty
ls /mnt/arch/usr/lib/modules            # should only show 7.0.0-38-generic
ls -l /mnt/arch/etc/resolv.conf         # symlink to ../run/systemd/resolve/stub-resolv.conf
```

All of these passed.

## Step 6: Add the GRUB entry

Append to the end of `/etc/grub.d/40_custom` (keep any existing entries):

```
menuentry 'Arch Linux ARM' {
    search --no-floppy --fs-uuid --set=root c8b9b28b-eab1-4441-a158-027c42288db5
    linux /boot/vmlinuz-7.0.0-38-generic root=UUID=ef3b53f1-ab9a-4ca0-a1eb-12b9fa018402 rw clk_ignore_unused pd_ignore_unused cma=128M efi=noruntime
    initrd /boot/initrd.img-7.0.0-38-generic
}
```

```bash
sudo update-grub
sudo umount /mnt/arch
```

Notes:

- `search ... --set=root` points GRUB at the **Ubuntu** partition, which holds the kernel and initramfs.
- `root=UUID=` points the kernel at the **Arch** partition.
- The version is pinned (`7.0.0-38-generic`) instead of using the `vmlinuz` symlink, so it always matches the copied modules.
- Ubuntu's initramfs works for Arch because it only needs to find the root device (nvme, ext4) and `switch_root` into it.
- Ubuntu's own entries are untouched, so it is always possible to boot back into Ubuntu.

## Step 7: First boot and post-install

First boot reached a working shell. Networking came up through `enu1` (a wired/USB network adapter) via `systemd-networkd`, and pacman worked.

### Fix `IgnorePkg` placement

`IgnorePkg` was appended to the bottom of `/etc/pacman.conf`, under the `[aur]` section, where pacman ignores it (it printed `directive 'IgnorePkg' in section 'aur' not recognized`). Move it into `[options]`:

```bash
sudo sed -i '/^IgnorePkg = linux-aarch64/d' /etc/pacman.conf
sudo sed -i '/^\[options\]/a IgnorePkg = linux-aarch64 linux-aarch64-headers' /etc/pacman.conf
sudo pacman -Syu
```

Confirm the generic kernel is not installed:

```bash
sudo pacman -Q linux-aarch64      # should say "was not found"
grep -n IgnorePkg /etc/pacman.conf
```

### Wi-Fi

```bash
ip link
rfkill list
sudo dmesg | grep -iE 'ath12k|wlan|firmware' | tail -30
nmtui
```

Use NetworkManager for Wi-Fi. `iwd` was installed during testing; do not run it alongside NetworkManager (two Wi-Fi daemons conflict). Once Wi-Fi is confirmed, disable the duplicate network service:

```bash
sudo systemctl disable --now systemd-networkd
```

### Cleanup

```bash
sudo userdel -r alarm
sudo hostnamectl set-hostname <hostname>
sudo timedatectl set-timezone <Region/City>
# uncomment en_US.UTF-8 UTF-8 in /etc/locale.gen, then:
sudo locale-gen
```

### Desktop

```bash
sudo pacman -S mesa vulkan-freedreno plasma-meta konsole dolphin sddm
sudo systemctl enable sddm
```

The install completed (555 packages). The `qcom-battmgr ... failed to send synthetic uevent: -11` messages during the udev hook are harmless; udev poked the battery devices while the Qualcomm battery manager was busy.

**Status at the time of writing:** SDDM was enabled but not yet booted into. Result of the first graphical boot is not recorded here.

### If the graphical boot fails

- Press `Ctrl+Alt+F3` for a text console.
- Or at GRUB press `e` on the Arch entry, append `3` to the `linux` line, and press `Ctrl+X` to boot to a text console without starting SDDM.
- Gather logs:
  ```bash
  journalctl -b -u sddm
  sudo dmesg | grep -iE 'msm|drm|adreno'
  ```
- SDDM's default X11 greeter can be switched to Wayland, which suits Snapdragon X better.
- Escape hatch: `sudo systemctl disable sddm` returns to a text-mode boot.

---

## Maintenance: when Ubuntu updates its kernel

The Arch root depends on the Ubuntu kernel's exact modules. After every Ubuntu kernel update:

1. Boot Ubuntu and note the new version (`uname -r`).
2. Copy the new modules into the Arch root:
   ```bash
   sudo mount /dev/nvme0n1p7 /mnt/arch
   sudo cp -a /lib/modules/<new-version> /mnt/arch/usr/lib/modules/
   sudo cp -a /lib/firmware/. /mnt/arch/usr/lib/firmware/
   ```
3. Update the version numbers in the `Arch Linux ARM` entry in `/etc/grub.d/40_custom`, then run `sudo update-grub`.
4. Keep the old kernel's modules and entry until the new one is confirmed working.

Alternative: bind-mount or symlink Ubuntu's `/lib/modules` into the Arch root so there is a single source of truth.

## Caveats

- Do not delete `/usr/lib/firmware/qcom/...` in Arch. The laptop-specific blobs came from Ubuntu, and Arch's `linux-firmware` package may not include them.
- Arch's Mesa and Wayland are newer than Ubuntu's. This is usually fine or better, but occasionally breaks GPU acceleration on this hardware.
- Keep Secure Boot configured so the Ubuntu-signed GRUB and kernel keep loading.
- The Arch ARM `linux-aarch64` package must stay ignored and uninstalled. It does not boot this laptop.

## Quick reference

| Task | Command |
|---|---|
| Mount Arch root from Ubuntu | `sudo mount /dev/nvme0n1p7 /mnt/arch` |
| Safe chroot | see Step 4 (`--rbind` + `--make-rslave`) |
| Clean unmount | kill leftover `gpg-agent`/`dirmngr`, then `sudo umount -R /mnt/arch` |
| Regenerate GRUB | `sudo update-grub` |
| Boot to text console | add `3` to the `linux` line in GRUB (`e` to edit) |
| Disable the display manager | `sudo systemctl disable sddm` |


# Fix Wi-Fi issue
1. Boot into Ubuntu
First, verify that Wi-Fi still works on Ubuntu:
```bash
mcli device status
uname -r
```

Then identify the partitions:
```bash
lsblk -f
```

We need to find the Arch root partition.
2. Mount your Arch installation
For example, if Arch is installed on `/dev/nvme0n1p7`:
```bash
sudo mkdir -p /mnt/arch
sudo mount /dev/nvme0n1p7 /mnt/arch
```

Replace `/dev/nvme0n1p7` with your the Arch root partition. Don't guess the partition number.

If Arch uses Btrfs subvolumes, LUKS encryption, or a separate /usr partition, the mounting procedure may differ.
3. Compare the firmware directories
Before entering the chroot, we can compare Ubuntu's working firmware with Arch's.

On Ubuntu:
```bash
ls /lib/firmware/ath11k/
```

Then:
```bash
ls /mnt/arch/usr/lib/firmware/ath11k/
```

Since the HP OmniBook X 14 previously needed Qualcomm firmware adjustments, pay particular attention to:
```plaintext
ath11k/
└── WCN6855/
    ├── hw2.0/
    └── hw2.1/
```
We should also check for HP-specific firmware files.

If Ubuntu has firmware that Arch is missing, we can copy the relevant files.

Don't copy the entire firmware directory yet. Identify exactly which files Arch needs.
4. Enter the Arch chroot
Once the partition is mounted:
```bash
sudo mount --rbind /dev /mnt/arch/dev
sudo mount --make-rslave /mnt/arch/dev
sudo mount -t proc proc /mnt/arch/proc
sudo mount --rbind /sys /mnt/arch/sys
sudo mount --make-rslave /mnt/arch/sys
sudo mount --rbind /run /mnt/arch/run
sudo mount --make-rslave /mnt/arch/run
sudo chroot /mnt/arch /bin/bash
```
Once operating inside Arch's filesystem while Ubuntu's kernel is running.

You can inspect installed packages and modify Arch's firmware.

For example:
```bash
pacman -Q | grep firmware
```

One limitation: loading kernel modules inside the chroot affects the running Ubuntu kernel, not a future Arch boot. So avoid using modprobe as a test of whether Arch's Wi-Fi is fixed.

Now run these commands from Ubuntu:
```bash
ls -lah /lib/firmware/ath11k/WCN6855/hw2.1/
```

And:
```bash
ls -lah /mnt/arch/usr/lib/firmware/ath11k/WCN6855/hw2.1/
```

Also compare the hw2.0 directories:
```bash
ls -lah /lib/firmware/ath11k/WCN6855/hw2.0/
ls -lah /mnt/arch/usr/lib/firmware/ath11k/WCN6855/hw2.0/
```

From Ubuntu, outside the chroot, run:
```bash
sudo mkdir -p /mnt/arch/usr/lib/firmware/ath11k/WCN6855
```

First, back up Arch's existing firmware:
```bash
sudo cp -a \
  /mnt/arch/usr/lib/firmware/ath11k/WCN6855 \
  /mnt/arch/root/WCN6855-backup
```

Then copy Ubuntu's working firmware:
```bash
sudo cp -a \
  /lib/firmware/ath11k/WCN6855/. \
  /mnt/arch/usr/lib/firmware/ath11k/WCN6855/
```

This copies both hw2.0 and hw2.1, including the firmware modifications that made Ubuntu work.

It doesn't modify Ubuntu's firmware.

One caveat: If Arch has uncompressed firmware files alongside Ubuntu's compressed .zst files, copying may leave both versions present. We'll need to check for that if Wi-Fi still doesn't work.
Step 3: Check kernel modules
Because both installations use the same kernel version, check whether Arch has the necessary driver modules.

From Ubuntu:
```bash
ls /mnt/arch/usr/lib/modules/
```

Then:
```bash
find /mnt/arch/usr/lib/modules/ -name 'ath11k_pci.ko*'
```

If nothing appears, Arch may not have the appropriate modules installed, even though it's booting Ubuntu's kernel.

Reboot into arch and connect to wifi :)