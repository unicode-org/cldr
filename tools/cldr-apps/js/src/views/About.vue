<template>
    <div id="home">
        <p class='hang'>
                For more information about the Survey Tool, see
                <a href='http://www.unicode.org/cldr'>unicode.org/cldr</a>.
        </p>
        <h1>About this SurveyTool Installation</h1>
        <table class='aboutBox'>
            <thead>
                <th>
                    Property
                </th>
                <th>
                    Value
                </th>
            </thead>
            <tbody>
                <tr v-for="(v, k) in aboutData" :key="k">
                    <td class='aboutKey'>
                        {{ k }}
                    </td>
                    <td class='aboutValue'>
                        {{ v }}
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script>
export default {
    data: function() {
        return {
            aboutData: {
                wait: "Loading…"
            }
        }
    },
    created: function() {
        fetch("api/about")
        .then(r => r.json())
        .then(data => (this.aboutData = data))
    }
}
</script>

<style scoped>
    .aboutBox th, .aboutBox td {
        padding: 0.5em;
    }

    .aboutBox thead {
        border-bottom: 2px solid black;
    }

    .aboutBox tr:nth-child(even) {
        background-color: #fff
    }

    .aboutBox tr:nth-child(odd) {
        background-color: #ddd
    }

    .aboutBox .aboutKey {
        font-weight: bold;
    }

    .aboutBox .aboutValue {
        font-family: monospace;
        white-space: pre-wrap
    }
</style>