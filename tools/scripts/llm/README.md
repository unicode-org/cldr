# CLDR LLM Validator

- **GSoC Contributor:** [Preet Sojitra](https://github.com/preetsojitra2712)
- **Mentors:** [Younies](https://github.com/younies), [Annemarie](https://github.com/aeapple)
- **Tech Lead:** [Mark Davis](https://github.com/macchiati)
- **Doc Status:** _Draft_
- **Last Update:** 2025-08-24
- **Project Information:** [Leveraging LLMs to validate locale data](https://summerofcode.withgoogle.com/programs/2023/projects/lE93K0ho)

## Introduction

Unicode supports essential global functions—such as date/time formatting, text segmentation, and measurement conversions—by publishing over a million locale data entries. However, maintaining the quality of this vast dataset presents significant challenges.

In this document, we leverage large language models (LLMs) to automate quality control. By prompting an LLM to generate outputs comparable to those in CLDR/ICU, we will build an AI-powered classifier that flags entries deviating from expected patterns.
