#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
caches_tar="${DIR}/jshrink_caches.tar.gz"
caches_untarred="${DIR}/jshrink_caches"
map="${DIR}/cache_map.csv"

{
	rm -rf "${caches_untarred}"
	mkdir -p "${caches_untarred}"
	tar xzf "${caches_tar}" -C "${caches_untarred}"

	cat "${map}" | while read entry; do
		from=$(echo "${entry}" | cut -d, -f1)
		to=$(echo "${entry}" | cut -d, -f2)
		mkdir -p $(dirname "${to}")
		cp "${from}" "${to}"
	done
	rm -rf "${caches_untarred}"
}&>/dev/null
