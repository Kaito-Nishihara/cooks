/**
 * kitchen-service。レシピの編集内容と献立を受け持つ。役割は、book-service に問い合わせた結果を使う。
 * 機能ごとに recipeedit と menu のパッケージを置き、その下を presentation、application、domain、infrastructure に分ける
 * (「共通設計: サービスの共通構成」の 4.2)。
 */
package app.recipebook.kitchen;
