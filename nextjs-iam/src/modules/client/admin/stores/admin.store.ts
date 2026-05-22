import { create } from "zustand";

export type ModalType =
  | "createOAuthClient"
  | "editOAuthClient"
  | "deleteOAuthClient";

interface IAdminStore {
  type: ModalType | null;
  isOpen: boolean;
  trigger: number;
  triggerInModal: number;
  incrementTrigger: () => void;
  incrementInModalTrigger: () => void;
  onOpen: (props: { type: ModalType }) => void;
  onClose: () => void;
}

const _useAdminStore = create<IAdminStore>((set) => ({
  type: null,
  isOpen: false,
  trigger: 0,
  triggerInModal: 0,
  onOpen: ({ type }) =>
    set({
      isOpen: true,
      type,
    }),
  onClose: () =>
    set({
      type: null,
      isOpen: false,
      trigger: 0,
      triggerInModal: 0,
    }),
  incrementTrigger: () => set((state) => ({ trigger: state.trigger + 1 })),
  incrementInModalTrigger: () =>
    set((state) => ({ triggerInModal: state.triggerInModal + 1 })),
}));

export const useAdminStore = _useAdminStore;
export const adminStore = _useAdminStore;
