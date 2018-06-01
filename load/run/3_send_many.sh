#!/usr/bin/env bash
echo port_x: ${port_x}
echo Valid: ${Valid}
echo esc: ${esc}
echo
echo user_x: ${user_x}
echo user_y: ${user_y}
echo user_z: ${user_z}
echo
echo _address_x: ${_address_x}
echo _secret_x: ${_secret_x}
echo _address_y: ${_address_y}
echo _secret_y: ${_secret_y}
echo _address_z: ${_address_z}
echo _secret_z: ${_secret_z}
echo
cd ${esc}
echo ------------------------------ send_many -------------------------------
echo
(echo '{"run":"get_me"}';echo '{"run":"send_many", "wires":{"'${_address_y}'":"100","'${_address_z}'":"100"}}') | docker exec -i adshares_esc_1 esc -n${Valid}  -P${port_x} -Hesc.dock -A${_address_x} -s${_secret_x}