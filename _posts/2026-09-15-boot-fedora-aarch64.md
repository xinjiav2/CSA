---
layout: post
permalink: /fedora/
author: Jade
title: Booting Fedora on my Arm laptop
showReadingTime: true
toc: true
---

# Basic Information
System info:
HP OmniBook X 14 (Model 14-fe0xxx)
Snapdragon X Elite
16gb
1TB ssd
Win 11 Home, Ubuntu 26.04 LTS

Try to boot Fedora 44, KDE Plasma desktop env, 

4GB usb, flashed w/ rufus in dd mode.

In Fedora GRUB, countdown and boots into fedora workstation, however nothing happens

## Fix

As I already have a working Ubuntu install on my laptop, I can directly chainload off of my Ubuntu GRUB

1. Have usb plugged into laptop, run `lsblk -o NAME,LABEL,UUID,FSTYPE`
2. find uuid of `Fedora-KDE-Live-44`, in my case my uuid was `2026-04-22-14-19-37-00`
3. Add Fedora to the Ubuntu GRUB, run `sudo nano /etc/grub.d/40_custom`, paste the following text (replace 2026-04-22-14-19-37-00 with your uuid)
```
menuentry "Start Fedora Live via Ubuntu GRUB" --class fedora {
    insmod part_gpt
    insmod ext2
    search --no-floppy --fs-uuid --set=root 2026-04-22-14-19-37-00
    linux /boot/aarch64/loader/linux root=live:UUID=2026-04-22-14-19-37-00 rd.live.image quiet rhgb clk_ignore_unused pd_ignore_unused arm64.nopauth modprobe.blacklist=qcom_q6v5_pas systemd.tpm2_wait=0
    initrd /boot/aarch64/loader/initrd
}
```
4. update grub by running `sudo update-grub`

After this I restarted laptop, and on Ubuntu GRUB screen there is option "Start Fedora Live via Ubuntu GRUB", where I can boot Fedora via Ubuntu's GRUB
After this I continue w/ Fedora installation normally

