import type {APIRoute} from "astro";
import {getRuntime, PagesRuntime} from "@astrojs/cloudflare/runtime";


export function get({request}): APIRoute {
  const runtime = getRuntime(request) as PagesRuntime

  const id = runtime.env.WS_GATEWAY.idFromName('gateway')
  const object = runtime.env.WS_GATEWAY.get(id)
  return object.fetch(request)
}