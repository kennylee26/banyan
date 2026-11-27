/**
 * Excel 解析公共契约：推送式分批读取、批次处理器、行读取器 SPI、限制项与结果。
 *
 * <p>本包主依赖链零 Spring、零第三方 Excel 库，只依赖本库 commons；不注册 bean、不做自动装配，
 * 也不提供配置项。具体读取能力由实现模块提供。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
package cn.kennylee.banyan.excel.parser;
