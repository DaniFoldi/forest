import type {APIRoute} from "astro";
import {getRuntime, PagesRuntime} from "@astrojs/cloudflare/runtime";


export function get(): APIRoute {
  const runtime = getRuntime(Astro.request) as PagesRuntime

  const id = runtime.env.WS_GATEWAY.idFromName('gateway')
  const object = runtime.env.WS_GATEWAY.get(id)
  return object.fetch(Astro.request)
}