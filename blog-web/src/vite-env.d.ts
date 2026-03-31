/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 构建时由 vite.config 从仓库根目录 VERSION 注入 */
  readonly VITE_APP_VERSION: string
}
