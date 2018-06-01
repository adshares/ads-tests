#!/usr/bin/env bash
echo port_x: ${port_x}
echo Valid: ${Valid}
echo esc: ${esc}
echo
echo user_x: ${user_x}
echo
echo _address_x: ${_address_x}
echo _secret_x: ${_secret_x}
echo _public_key_x: ${_public_key_x}
echo
cd
cd ../..
cd ${esc}
echo ------------------------------ get_me -------------------------------
echo
(echo '{"run":"get_me"}'; echo '{"run":"broadcast","message":"'${_public_key_x}'"}') | docker exec -i adshares_esc_1 esc -n${Valid}  -P${port_x} -Hesc.dock -A${_address_x} -s${_secret_x}

