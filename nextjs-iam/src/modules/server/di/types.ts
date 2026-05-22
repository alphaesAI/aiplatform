import { IOAuthClientService } from "../core/admin/domain/interfaces/oauthclient.service.interface";
import { IAuthService } from "../core/auth/domain/interfaces/auth.service.interface";
import { IEmailService } from "../core/common/email/domain/interfaces/email.service.interface";

export const DI_SYMBOLS = {
  // Repositories

  // Services
  IAuthService: Symbol.for("IAuthService"),
  IEmailService: Symbol.for("IEmailService"),
  IOAuthClientService: Symbol.for("IOAuthClientService"),
};

export interface DI_RETURN_TYPES {
  // Repositories

  // Services
  IAuthService: IAuthService;
  IEmailService: IEmailService;
  IOAuthClientService: IOAuthClientService;
}
