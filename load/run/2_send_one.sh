#!/usr/bin/env bash
echo port_y: ${port_y}
echo Valid: ${Valid}
echo esc: ${esc}
echo
echo user_x: ${user_x}
echo user_y: ${user_y}
echo
echo _address_x: ${_address_x}
echo _secret_x: ${_secret_x}
echo _address_y: ${_address_y}
echo _secret_y: ${_secret_y}
echo
cd ${esc}
echo ------------------------------ send_one -------------------------------
echo
(echo '{"run":"get_me"}';echo '{"run":"send_one", "address":"'${_address_x}'", "amount":"100"}') | docker exec -i adshares_esc_1 esc -n${Valid}  -P${port_y} -Hesc.dock -A${_address_y} -s${_secret_y}