Loosely inspired by [Ramirez57/IPAuth](https://github.com/Ramirez57/IPAuth).
This simple plugin authorizes player logins based on their IPv4/IPv6 address check against a list
of allowed networks in CIDR format. It can prevent players from using
shared computers, changing their physical location, or accessing server
without reserved VPN slot.
<i>:warning: This plugin is not intended as standalone
security solution!</i>

<b>:speech_balloon: Command usage</b>
* `/sipauth list [player]` prints currently allowed networks [of player]<br>
* `/sipauth add 192.168.5.0/24 [player]` adds 192.168.5.0/24 to list of allowed networks [of player]<br>
* `/sipauth remove 10.0.0.0/8 [player]` removes 10.0.0.0/8 from list of allowed networks [of player]<br>
* `/sipauth reload` reloads all player networks

If single address is provided, it assumes `/32` network mask (IPv4)
or `/128` host prefix (IPv6). First login is always allowed and
creates a new configuration with current single-host address.

<b>:vertical_traffic_light: Defined permissions</b>
* `sipauth.bypass` default false, skips any IP checks,<br>
* `sipauth.reload` default op, reloads all plugin configuration,<br>
* `sipauth.manage` default op, can use `add`/`remove`/`list` on other players<br>
