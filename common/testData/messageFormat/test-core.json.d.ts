// Copyright © 1991-2024 Unicode, Inc.
// For terms of use, see http://www.unicode.org/copyright.html
// SPDX-License-Identifier: Unicode-3.0

export type TestMessage = {
 /** The MF2 message to be tested. */
  src: string;
  /** The locale to use for formatting. Defaults to 'en-US'. */
  locale?: string;
  /** Parameters to pass in to the formatter for resolving external variables. */
  params?: Record<string, string | number | null | undefined>;
  /** The expected result of formatting the message to a string. */
  exp: string;
  /** The expected result of formatting the message to parts. */
  parts?: Array<object>;
  /** A normalixed form of `src`, for testing stringifiers. */
  cleanSrc?: string;
  /** The runtime errors expected to be emitted when formatting the message. */
  errors?: Array<{ type: string }>;
  /** Normally not set. A flag to use during development to only run one or more specific tests. */
  only?: boolean;
};

declare const data: TestMessage[];
export default data;
