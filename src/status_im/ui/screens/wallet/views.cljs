(ns status-im.ui.screens.wallet.views
  (:require-macros [status-im.utils.views :as views])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.ui.screens.wallet.onboarding.views :as onboarding.views]
            [status-im.ui.screens.wallet.styles :as styles]
            [status-im.ui.screens.wallet.utils :as wallet.utils]
            [status-im.utils.ethereum.core :as ethereum]
            [status-im.utils.ethereum.tokens :as tokens]))

(defn toolbar-view []
  [toolbar/toolbar {:style styles/toolbar :flat? true}
   nil
   [toolbar/content-wrapper]
   [toolbar/actions
    [{:icon      :icons/options
      :icon-opts {:color               :white
                  :accessibility-label :options-menu-button}
      :options   [{:label  (i18n/label :t/wallet-manage-assets)
                   :action #(re-frame/dispatch [:navigate-to-modal :wallet-settings-assets])}]}]]])

(defn- total-section [value currency]
  [react/view styles/section
   [react/view {:style styles/total-balance-container}
    [react/view {:style styles/total-balance}
     [react/text {:style               styles/total-balance-value
                  :accessibility-label :total-amount-value-text}
      value]
     [react/text {:style               styles/total-balance-currency
                  :accessibility-label :total-amount-currency-text}
      (:code currency)]]
    [react/text {:style styles/total-value} (i18n/label :t/wallet-total-value)]]])

(def actions
  [{:label               (i18n/label :t/send-transaction)
    :accessibility-label :send-transaction-button
    :icon                :icons/arrow-right
    :action              #(re-frame/dispatch [:navigate-to :wallet-send-transaction])}
   {:label               (i18n/label :t/receive-transaction)
    :accessibility-label :receive-transaction-button
    :icon                :icons/arrow-left
    :action              #(re-frame/dispatch [:navigate-to :wallet-request-transaction])}
   {:label               (i18n/label :t/transaction-history)
    :accessibility-label :transaction-history-button
    :icon                :icons/transaction-history
    :action              #(re-frame/dispatch [:navigate-to :transactions-history])}])

(defn- render-asset [currency]
  (fn [{:keys [symbol icon decimals amount]}]
    (let [asset-value (re-frame/subscribe [:asset-value symbol (-> currency :code keyword)])]
      [react/view {:style styles/asset-item-container}
       [list/item
        [list/item-image icon]
        [react/view {:style styles/asset-item-value-container}
         [react/text {:style               styles/asset-item-value
                      :number-of-lines     1
                      :ellipsize-mode      :tail
                      :accessibility-label (str (-> symbol name clojure.string/lower-case) "-asset-value-text")}
          (wallet.utils/format-amount amount decimals)]
         [react/text {:style           styles/asset-item-currency
                      :uppercase?      true
                      :number-of-lines 1}
          (clojure.core/name symbol)]]
        [react/text {:style           styles/asset-item-price
                     :uppercase?      true
                     :number-of-lines 1}
         (if @asset-value (str (:symbol currency) @asset-value) "...")]]])))

(defn- asset-section [assets currency]
  [react/view styles/asset-section
   [react/text {:style styles/asset-section-title} (i18n/label :t/wallet-assets)]
   [list/flat-list
    {:default-separator? true
     :scroll-enabled     false
     :key-fn             (comp str :symbol)
     :data               assets
     :render-fn          (render-asset currency)}]])

(views/defview wallet-root []
  (views/letsubs [assets          [:wallet/visible-assets-with-amount]
                  currency        [:wallet/currency]
                  portfolio-value [:portfolio-value]]
    [react/view styles/main-section
     [toolbar-view]
     [react/scroll-view {:refresh-control
                         (reagent/as-element
                          [react/refresh-control {:on-refresh #(re-frame/dispatch [:update-wallet])
                                                  :tint-color :white
                                                  :refreshing false}])}
      [react/view {:style styles/scroll-top}] ;; Hack to allow different colors for top / bottom scroll view]
      [total-section portfolio-value currency]
      [list/action-list actions
       {:container-style styles/action-section}]
      [asset-section assets currency]]]))

(views/defview wallet []
  (views/letsubs [{:keys [wallet-set-up-passed?]} [:get-current-account]]
    (if wallet-set-up-passed?
      [wallet-root]
      [onboarding.views/onboarding])))