import { createContainer } from "@evyweb/ioctopus"
import { DI_RETURN_TYPES, DI_SYMBOLS } from "./types"
import { registerAuthModule, registerEmailModule, registerOAuthClientModule } from "./modules"

const ApplicationContainer = createContainer()

registerAuthModule(ApplicationContainer)
registerEmailModule(ApplicationContainer)
registerOAuthClientModule(ApplicationContainer);

export const getInjection = <K extends keyof typeof DI_SYMBOLS>(
  symbol: K
): DI_RETURN_TYPES[K] => {
  return ApplicationContainer.get(DI_SYMBOLS[symbol])
}
