Inspired by Ramirez57/IPAuth. This plugin authorizes player logins based on their IPv4/IPv6 address check against a list
of allowed networks in CIDR format.
<i>This plugin is not intended as standalone security solution!</i>

Part of KonAuth project.

<b>Command usage</b>
* `/sipauth list [player]` prints currently allowed networks [of player]<br>
* `/siaputh add 192.168.5.0/24 [player]` adds 192.168.5.0/24 to list of allowed networks [of player]<br>
* `/sipauth remove 10.0.0.0/8 [player]` removes 10.0.0.0/8 from list of allowed networkd [of player]<br>
* `/sipauth reload` reloads all player networks

If single address is provided, it assumes `/32` network mask (IPv4)
or `/128` host prefix (IPv6). First player login creates a new configuration with current single-host address.

<b>Defined permissions</b>
* `sipauth.bypass` default false, skips IP check<br>
* `sipauth.reload` default op, <br>
* `sipauth.manage` default op, can use add/remove on other players<br>
